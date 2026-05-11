#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import sys
import time
import uuid
from pathlib import Path
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlparse

from pymilvus import DataType, MilvusClient
from qdrant_client import QdrantClient
from qdrant_client import models
from tqdm import tqdm
import yaml

HTTPS = "https"
DEFAULT_HTTP_PORT = 80
DEFAULT_HTTPS_PORT = 443
DEFAULT_OFFSETS_COLLECTION = "_migration_offsets"
MAX_UPSERT_RETRIES = 5
BASE_RETRY_DELAY_SECONDS = 0.2
CONFIG_PATH = Path(__file__).with_name("config.yaml")
QDRANT_TIMEOUT_SECONDS = 300


class MigrationError(Exception):
    pass


@dataclass
class Globals:
    debug: bool
    trace: bool
    skip_tls_verification: bool


@dataclass
class MigrationConfig:
    batch_size: int
    restart: bool
    create_collection: bool
    offsets_collection: str
    batch_delay_ms: int


@dataclass
class MilvusConfig:
    url: str
    collection: str
    api_key: str | None
    enable_tls_auth: bool
    username: str | None
    password: str | None
    db_name: str | None
    server_version: str | None
    partitions: list[str]


@dataclass
class MilvusDBConfig:
    url: str
    api_key: str | None
    enable_tls_auth: bool
    username: str | None
    password: str | None
    db_name: str | None
    server_version: str | None
    partitions: list[str]


@dataclass
class QdrantConfig:
    url: str
    api_key: str | None
    collection: str


@dataclass
class QdrantTargetConfig:
    url: str
    api_key: str | None


def now_rfc3339() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def normalize_url(raw_url: str, use_tls: bool) -> str:
    if "://" in raw_url:
        return raw_url
    scheme = "https" if use_tls else "http"
    return f"{scheme}://{raw_url}"


def parse_qdrant_url(url_str: str) -> tuple[str, int, bool]:
    parsed = urlparse(url_str)
    if not parsed.scheme or not parsed.hostname:
        raise MigrationError(f"failed to parse URL: {url_str}")
    if parsed.port is not None:
        port = parsed.port
    elif parsed.scheme == HTTPS:
        port = DEFAULT_HTTPS_PORT
    else:
        port = DEFAULT_HTTP_PORT
    return parsed.hostname, port, parsed.scheme == HTTPS


def validate_batch_size(batch_size: int) -> None:
    if batch_size < 1:
        raise MigrationError(f"batch size must be greater than 0, got: {batch_size}")


def load_config() -> dict[str, Any]:
    if not CONFIG_PATH.exists():
        raise MigrationError(f"required config file not found: {CONFIG_PATH}")
    try:
        with CONFIG_PATH.open("r", encoding="utf-8") as handle:
            data = yaml.safe_load(handle) or {}
    except Exception as exc:
        raise MigrationError(f"failed to read config file {CONFIG_PATH}: {exc}") from exc
    if not isinstance(data, dict):
        raise MigrationError(f"config file must contain a YAML object: {CONFIG_PATH}")
    return data


def deep_get(config: dict[str, Any], *keys: str) -> Any:
    current: Any = config
    for key in keys:
        if not isinstance(current, dict):
            return None
        current = current.get(key)
    return current


def get_arg_or_config(args: argparse.Namespace, attr: str, config: dict[str, Any], *keys: str, default: Any = None) -> Any:
    value = getattr(args, attr, None)
    if value is not None:
        return value
    config_value = deep_get(config, *keys)
    if config_value is not None:
        return config_value
    return default


def parse_distance_metrics(entries: list[str] | None) -> dict[str, str]:
    metrics: dict[str, str] = {}
    if not entries:
        return metrics
    for entry in entries:
        if "=" not in entry:
            raise MigrationError(f"invalid --qdrant.distance-metric value: {entry!r}, expected name=metric")
        field_name, metric = entry.split("=", 1)
        field_name = field_name.strip()
        metric = metric.strip()
        if not field_name or not metric:
            raise MigrationError(f"invalid --qdrant.distance-metric value: {entry!r}, expected name=metric")
        metrics[field_name] = metric
    return metrics


def display_migration_start(source_provider: str, source_collection: str, target_collection: str) -> None:
    print("Starting Migration To Qdrant")
    print(f"From -> To: {source_collection}@{source_provider} -> {target_collection}@qdrant")
    print()


def display_migration_progress(progress: tqdm, offset_count: int) -> None:
    if offset_count > 0:
        print(f"Starting from offset {offset_count}")
        progress.update(offset_count)
    else:
        print("Starting from the beginning")
    print()


def arbitrary_id_to_uuid(value: str) -> str:
    try:
        parsed = uuid.UUID(value)
        return str(parsed)
    except ValueError:
        return str(uuid.uuid5(uuid.NAMESPACE_URL, value))


def offset_point_id(source_collection: str) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_URL, source_collection))


def is_retryable_error(exc: Exception) -> bool:
    err_str = str(exc)
    return (
        "Please retry" in err_str
        or "too_many_internal_resets" in err_str
        or "RST_STREAM" in err_str
        or "connection error" in err_str
        or "ResourceExhausted" in err_str
    )


def is_collection_not_loaded_error(exc: Exception) -> bool:
    err_str = str(exc).lower()
    return "collection not loaded" in err_str


def is_collection_recovering_error(exc: Exception) -> bool:
    err_str = str(exc).lower()
    return "collection on recovering" in err_str or "may be in recovery" in err_str


def upsert_with_retry(client: QdrantClient, collection_name: str, points: list[models.PointStruct]) -> None:
    last_error: Exception | None = None
    for attempt in range(MAX_UPSERT_RETRIES):
        try:
            client.upsert(collection_name=collection_name, points=points, wait=True)
            return
        except Exception as exc:  # pragma: no cover - library/runtime path
            last_error = exc
            if not is_retryable_error(exc):
                break
            time.sleep(BASE_RETRY_DELAY_SECONDS * (2**attempt))
    raise MigrationError(f"failed to insert data into target: {last_error}")


def connect_to_milvus(config: MilvusConfig | MilvusDBConfig) -> MilvusClient:
    kwargs: dict[str, Any] = {
        "uri": normalize_url(config.url, config.enable_tls_auth),
    }
    if config.db_name:
        kwargs["db_name"] = config.db_name
    if config.api_key:
        kwargs["token"] = config.api_key
    elif config.username and config.password:
        kwargs["user"] = config.username
        kwargs["password"] = config.password
    return MilvusClient(**kwargs)


def connect_to_qdrant(config: QdrantConfig | QdrantTargetConfig, globals_cfg: Globals) -> QdrantClient:
    host, port, use_tls = parse_qdrant_url(normalize_url(config.url, False))
    kwargs: dict[str, Any] = {
        "host": host,
        "grpc_port": port,
        "prefer_grpc": True,
        "https": use_tls,
        "api_key": config.api_key,
        "timeout": QDRANT_TIMEOUT_SECONDS,
    }
    if globals_cfg.skip_tls_verification:
        kwargs["verify"] = False
    return QdrantClient(**kwargs)


def collection_exists(client: QdrantClient, collection_name: str) -> bool:
    return bool(client.collection_exists(collection_name))


def prepare_offsets_collection(client: QdrantClient, collection_name: str) -> None:
    if collection_exists(client, collection_name):
        return
    client.create_collection(collection_name=collection_name, vectors_config={})


def delete_offsets_collection(client: QdrantClient, collection_name: str) -> None:
    if not collection_exists(client, collection_name):
        print(f"Collection {collection_name} does not exist, nothing to delete")
        return
    client.delete_collection(collection_name)


def get_start_offset(client: QdrantClient, offsets_collection: str, source_collection: str) -> tuple[int | str | None, int]:
    points = client.retrieve(
        collection_name=offsets_collection,
        ids=[offset_point_id(source_collection)],
        with_payload=True,
        with_vectors=False,
    )
    if not points:
        return None, 0
    payload = points[0].payload or {}
    offset_key = f"{source_collection}_offset"
    offset_count_key = f"{source_collection}_offsetCount"
    if offset_key not in payload or offset_count_key not in payload:
        return None, 0
    offset = payload[offset_key]
    offset_count = payload[offset_count_key]
    if not isinstance(offset_count, int):
        raise MigrationError("failed to get offset count: invalid type")
    if isinstance(offset, (int, str)):
        return offset, offset_count
    return None, 0


def store_start_offset(
    client: QdrantClient,
    offsets_collection: str,
    source_collection: str,
    offset: int | str | None,
    offset_count: int,
) -> None:
    if offset is None:
        return
    payload = {
        f"{source_collection}_offset": offset,
        f"{source_collection}_offsetCount": offset_count,
        f"{source_collection}_lastUpsertAt": now_rfc3339(),
    }
    point = models.PointStruct(
        id=offset_point_id(source_collection),
        vector={},
        payload=payload,
    )
    client.upsert(collection_name=offsets_collection, points=[point], wait=True)


def get_offset_payload(client: QdrantClient, offsets_collection: str, source_collection: str) -> dict[str, Any]:
    points = client.retrieve(
        collection_name=offsets_collection,
        ids=[offset_point_id(source_collection)],
        with_payload=True,
        with_vectors=False,
    )
    if not points:
        return {}
    return dict(points[0].payload or {})


def is_collection_completed(client: QdrantClient, offsets_collection: str, source_collection: str) -> bool:
    payload = get_offset_payload(client, offsets_collection, source_collection)
    return payload.get(f"{source_collection}_status") == "completed"


def mark_collection_completed(client: QdrantClient, offsets_collection: str, source_collection: str) -> None:
    payload = get_offset_payload(client, offsets_collection, source_collection)
    payload[f"{source_collection}_status"] = "completed"
    payload[f"{source_collection}_completedAt"] = now_rfc3339()
    point = models.PointStruct(
        id=offset_point_id(source_collection),
        vector={},
        payload=payload,
    )
    client.upsert(collection_name=offsets_collection, points=[point], wait=True)


def milvus_type_name(field_type: Any) -> str:
    try:
        return DataType(field_type).name
    except Exception:
        return str(field_type)


def normalize_scalar_value(field_type: Any, value: Any) -> Any:
    data_type = DataType(field_type)
    if data_type == DataType.BOOL:
        return bool(value)
    if data_type in (DataType.INT8, DataType.INT16, DataType.INT32, DataType.INT64):
        return int(value)
    if data_type == DataType.FLOAT:
        return float(value)
    if data_type == DataType.DOUBLE:
        return float(value)
    if data_type in (DataType.VARCHAR, DataType.STRING):
        return str(value)
    if data_type == DataType.JSON:
        if isinstance(value, (bytes, bytearray, str)):
            decoded = json.loads(value)
        else:
            decoded = value
        if not isinstance(decoded, dict):
            raise MigrationError("failed to parse JSON: expected object")
        return decoded
    if data_type == DataType.FLOAT_VECTOR:
        return [float(item) for item in value]
    raise MigrationError(f"unsupported field type: {milvus_type_name(field_type)}")


def build_field_type_map(schema: dict[str, Any]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for field in schema.get("fields", []):
        result[field["name"]] = field["type"]
    return result


def get_pk_field(schema: dict[str, Any]) -> dict[str, Any]:
    for field in schema.get("fields", []):
        if field.get("is_primary"):
            return field
    raise MigrationError("failed to locate Milvus primary key field")


def count_milvus_vectors(client: MilvusClient, collection_name: str) -> int:
    stats = client.get_collection_stats(collection_name=collection_name)
    return int(stats["row_count"])


def prepare_target_collection(
    milvus_client: MilvusClient,
    qdrant_client: QdrantClient,
    milvus_collection: str,
    qdrant_collection: str,
    create_collection: bool,
    distance_metric_overrides: dict[str, str],
) -> None:
    if not create_collection:
        return
    if collection_exists(qdrant_client, qdrant_collection):
        print(f"Target collection {qdrant_collection!r} already exists. Skipping creation.")
        return

    schema = milvus_client.describe_collection(collection_name=milvus_collection)
    distance_mapping = {
        "euclid": models.Distance.EUCLID,
        "cosine": models.Distance.COSINE,
        "dot": models.Distance.DOT,
        "manhattan": models.Distance.MANHATTAN,
    }
    vectors_config: dict[str, models.VectorParams] = {}
    vector_metrics = get_milvus_vector_metrics(milvus_client, milvus_collection)
    scalar_indexes = get_milvus_scalar_indexes(milvus_client, milvus_collection)
    pk_field = get_pk_field(schema)
    pk_name = pk_field["name"]

    for field in schema.get("fields", []):
        field_type = DataType(field["type"])
        if field_type != DataType.FLOAT_VECTOR:
            continue
        params = field.get("params", {})
        dim_raw = params.get("dim")
        if dim_raw is None:
            raise MigrationError(f"failed to parse vector dimension for field {field['name']!r}")
        dimension = int(dim_raw)

        distance_metric = vector_metrics.get(field["name"], "cosine")
        if field["name"] in distance_metric_overrides:
            specified = distance_metric_overrides[field["name"]]
            if specified not in distance_mapping:
                raise MigrationError(
                    f"invalid distance metric {specified!r} for vector {field['name']!r}"
                )
            distance_metric = specified

        vectors_config[field["name"]] = models.VectorParams(
            size=dimension,
            distance=distance_mapping[distance_metric],
        )

    qdrant_client.create_collection(
        collection_name=qdrant_collection,
        vectors_config=vectors_config,
    )
    create_qdrant_payload_indexes(
        qdrant_client=qdrant_client,
        qdrant_collection=qdrant_collection,
        schema=schema,
        scalar_indexes=scalar_indexes,
        pk_name=pk_name,
    )
    print(f"Created target collection {qdrant_collection!r}")


def get_milvus_vector_metrics(client: MilvusClient, collection_name: str) -> dict[str, str]:
    metric_mapping = {
        "L2": "euclid",
        "IP": "dot",
        "COSINE": "cosine",
    }
    vector_metrics: dict[str, str] = {}
    try:
        index_names = client.list_indexes(collection_name=collection_name)
    except Exception:
        return vector_metrics

    for index_name in index_names:
        try:
            index_info = client.describe_index(collection_name=collection_name, index_name=index_name)
        except Exception:
            continue
        field_name = index_info.get("field_name")
        metric_type = index_info.get("metric_type")
        if not field_name or not metric_type:
            continue
        mapped_metric = metric_mapping.get(str(metric_type).upper())
        if mapped_metric:
            vector_metrics[str(field_name)] = mapped_metric
    return vector_metrics


def get_milvus_scalar_indexes(client: MilvusClient, collection_name: str) -> dict[str, str]:
    scalar_indexes: dict[str, str] = {}
    try:
        index_names = client.list_indexes(collection_name=collection_name)
    except Exception:
        return scalar_indexes

    for index_name in index_names:
        try:
            index_info = client.describe_index(collection_name=collection_name, index_name=index_name)
        except Exception:
            continue
        field_name = index_info.get("field_name")
        index_type = index_info.get("index_type")
        if not field_name or not index_type:
            continue
        scalar_indexes[str(field_name)] = str(index_type).upper()
    return scalar_indexes


def qdrant_payload_schema_for_milvus_type(field_type: Any) -> Any | None:
    data_type = DataType(field_type)
    if data_type == DataType.BOOL:
        return models.PayloadSchemaType.BOOL
    if data_type in (DataType.INT8, DataType.INT16, DataType.INT32, DataType.INT64):
        return models.PayloadSchemaType.INTEGER
    if data_type in (DataType.FLOAT, DataType.DOUBLE):
        return models.PayloadSchemaType.FLOAT
    if data_type in (DataType.VARCHAR, DataType.STRING):
        return models.PayloadSchemaType.KEYWORD
    return None


def create_qdrant_payload_indexes(
    qdrant_client: QdrantClient,
    qdrant_collection: str,
    schema: dict[str, Any],
    scalar_indexes: dict[str, str],
    pk_name: str,
) -> None:
    for field in schema.get("fields", []):
        field_name = str(field["name"])
        if field_name == pk_name:
            continue
        if scalar_indexes.get(field_name) != "AUTOINDEX":
            continue

        field_schema = qdrant_payload_schema_for_milvus_type(field["type"])
        if field_schema is None:
            continue

        qdrant_client.create_payload_index(
            collection_name=qdrant_collection,
            field_name=field_name,
            field_schema=field_schema,
            wait=True,
        )


def build_filter(pk_name: str, pk_type: Any, offset: int | str | None) -> str:
    if offset is None:
        return ""
    data_type = DataType(pk_type)
    if data_type == DataType.INT64:
        return f"{pk_name} > {offset}"
    if data_type in (DataType.VARCHAR, DataType.STRING):
        return f"{pk_name} > '{offset}'"
    return ""


def migrate_collection(
    globals_cfg: Globals,
    milvus_config: MilvusConfig,
    qdrant_config: QdrantConfig,
    migration_config: MigrationConfig,
    distance_metric_overrides: dict[str, str],
    cleanup_offsets_on_success: bool = True,
    mark_completed_on_success: bool = False,
) -> None:
    validate_batch_size(migration_config.batch_size)

    milvus_client = connect_to_milvus(milvus_config)
    qdrant_client = connect_to_qdrant(qdrant_config, globals_cfg)
    try:
        prepare_offsets_collection(qdrant_client, migration_config.offsets_collection)

        source_point_count = count_milvus_vectors(milvus_client, milvus_config.collection)
        prepare_target_collection(
            milvus_client=milvus_client,
            qdrant_client=qdrant_client,
            milvus_collection=milvus_config.collection,
            qdrant_collection=qdrant_config.collection,
            create_collection=migration_config.create_collection,
            distance_metric_overrides=distance_metric_overrides,
        )

        display_migration_start("milvus", milvus_config.collection, qdrant_config.collection)

        offset_value: int | str | None = None
        offset_count = 0
        if not migration_config.restart:
            offset_value, offset_count = get_start_offset(
                qdrant_client,
                migration_config.offsets_collection,
                milvus_config.collection,
            )

        schema = milvus_client.describe_collection(collection_name=milvus_config.collection)
        pk_field = get_pk_field(schema)
        pk_name = pk_field["name"]
        pk_type = pk_field["type"]
        field_type_map = build_field_type_map(schema)

        progress = tqdm(total=source_point_count, unit="pt")
        try:
            display_migration_progress(progress, offset_count)

            while True:
                filter_expr = build_filter(pk_name, pk_type, offset_value)
                rows = milvus_client.query(
                    collection_name=milvus_config.collection,
                    filter=filter_expr,
                    output_fields=["*"],
                    partition_names=milvus_config.partitions or None,
                    limit=migration_config.batch_size,
                )
                if not rows:
                    break

                target_points: list[models.PointStruct] = []
                for row in rows:
                    vectors: dict[str, list[float]] = {}
                    payload: dict[str, Any] = {}
                    point_id: int | str | None = None

                    for field_name, value in row.items():
                        field_type = field_type_map.get(field_name)
                        if field_name == pk_name:
                            normalized_pk = normalize_scalar_value(field_type, value)
                            if DataType(pk_type) == DataType.INT64:
                                offset_value = int(normalized_pk)
                                point_id = int(normalized_pk)
                            elif DataType(pk_type) in (DataType.VARCHAR, DataType.STRING):
                                offset_value = str(normalized_pk)
                                point_id = arbitrary_id_to_uuid(str(normalized_pk))
                            else:
                                raise MigrationError(
                                    f"unsupported field type: {milvus_type_name(pk_type)}"
                                )
                            continue

                        if field_type is None:
                            payload[field_name] = value
                            continue

                        normalized_value = normalize_scalar_value(field_type, value)
                        if DataType(field_type) == DataType.FLOAT_VECTOR:
                            vectors[field_name] = normalized_value
                            continue

                        if field_name == "metadata" and isinstance(normalized_value, dict):
                            for key, metadata_value in normalized_value.items():
                                payload[key] = metadata_value
                            continue

                        payload[field_name] = normalized_value

                    if point_id is None:
                        raise MigrationError("failed to resolve point ID from Milvus row")

                    target_points.append(
                        models.PointStruct(
                            id=point_id,
                            vector=vectors,
                            payload=payload,
                        )
                    )

                upsert_with_retry(qdrant_client, qdrant_config.collection, target_points)
                offset_count += len(target_points)
                store_start_offset(
                    qdrant_client,
                    migration_config.offsets_collection,
                    milvus_config.collection,
                    offset_value,
                    offset_count,
                )
                progress.update(len(target_points))

                if migration_config.batch_delay_ms > 0:
                    time.sleep(migration_config.batch_delay_ms / 1000.0)

            print("Data migration finished successfully")
        finally:
            progress.close()

        target_point_count = qdrant_client.count(collection_name=qdrant_config.collection, exact=True).count
        print(f"Target collection has {target_point_count} points")
        print()

        if mark_completed_on_success:
            mark_collection_completed(qdrant_client, migration_config.offsets_collection, milvus_config.collection)

        if cleanup_offsets_on_success:
            delete_offsets_collection(qdrant_client, migration_config.offsets_collection)
    finally:
        try:
            milvus_client.close()
        except Exception:
            pass
        try:
            qdrant_client.close()
        except Exception:
            pass


def migrate_database(
    globals_cfg: Globals,
    milvus_config: MilvusDBConfig,
    qdrant_config: QdrantTargetConfig,
    migration_config: MigrationConfig,
    distance_metric_overrides: dict[str, str],
    collection_prefix: str,
    report_file: str | None,
) -> None:
    report: dict[str, Any] = {
        "source_db": milvus_config.db_name,
        "source_url": milvus_config.url,
        "target_url": qdrant_config.url,
        "collection_prefix": collection_prefix,
        "started_at": now_rfc3339(),
        "finished_at": "",
        "status": "running",
        "successful": [],
        "failed": [],
    }

    milvus_client = connect_to_milvus(milvus_config)
    qdrant_client = connect_to_qdrant(qdrant_config, globals_cfg)
    try:
        print("Milvus DB to Qdrant Data Migration")
        prepare_offsets_collection(qdrant_client, migration_config.offsets_collection)
        collections = milvus_client.list_collections()
        names = [name for name in collections if str(name).strip()]
        if not names:
            print("No collections found in the selected Milvus database.")
            return

        print(f"Found {len(names)} Milvus collections in database {milvus_config.db_name!r}")

        for name in names:
            target_collection = f"{collection_prefix}{name}"
            if is_collection_completed(qdrant_client, migration_config.offsets_collection, name):
                print(f"Skipping Milvus collection {name!r}: already completed in previous run")
                report["successful"].append(
                    {
                        "source_collection": name,
                        "target_collection": target_collection,
                    }
                )
                continue
            print(f"Migrating Milvus collection {name!r} to Qdrant collection {target_collection!r}")
            try:
                migrate_collection(
                    globals_cfg=globals_cfg,
                    milvus_config=MilvusConfig(
                        url=milvus_config.url,
                        collection=name,
                        api_key=milvus_config.api_key,
                        enable_tls_auth=milvus_config.enable_tls_auth,
                        username=milvus_config.username,
                        password=milvus_config.password,
                        db_name=milvus_config.db_name,
                        server_version=milvus_config.server_version,
                        partitions=milvus_config.partitions,
                    ),
                    qdrant_config=QdrantConfig(
                        url=qdrant_config.url,
                        api_key=qdrant_config.api_key,
                        collection=target_collection,
                    ),
                    migration_config=migration_config,
                    distance_metric_overrides=distance_metric_overrides,
                    cleanup_offsets_on_success=False,
                    mark_completed_on_success=True,
                )
            except Exception as exc:
                report["failed"].append(
                    {
                        "source_collection": name,
                        "target_collection": target_collection,
                        "error": str(exc),
                    }
                )
                if is_collection_not_loaded_error(exc):
                    print(f"Skipping Milvus collection {name!r}: not loaded in Milvus")
                    continue
                if is_collection_recovering_error(exc):
                    print(f"Skipping Milvus collection {name!r}: Milvus collection is recovering")
                    continue
                raise
            report["successful"].append(
                {
                    "source_collection": name,
                    "target_collection": target_collection,
                }
            )
    except Exception as exc:
        report["status"] = "failed"
        report["finished_at"] = now_rfc3339()
        body = json.dumps(report, indent=2, ensure_ascii=True)
        print(body)
        if report_file:
            try:
                with open(report_file, "w", encoding="utf-8") as handle:
                    handle.write(body)
                print(f"Migration report written to {report_file!r}")
            except Exception as write_exc:
                print(f"Failed to write migration report {report_file!r}: {write_exc}", file=sys.stderr)
        print(
            f"Successful collections: {len(report['successful'])}, Failed collections: {len(report['failed'])}"
        )
        raise MigrationError(f"failed to migrate Milvus collection: {exc}") from exc
    finally:
        try:
            milvus_client.close()
        except Exception:
            pass
        try:
            qdrant_client.close()
        except Exception:
            pass

    report["status"] = "completed"
    report["finished_at"] = now_rfc3339()
    body = json.dumps(report, indent=2, ensure_ascii=True)
    print(body)
    cleanup_client = connect_to_qdrant(qdrant_config, globals_cfg)
    try:
        delete_offsets_collection(cleanup_client, migration_config.offsets_collection)
    finally:
        try:
            cleanup_client.close()
        except Exception:
            pass
    if report_file:
        try:
            with open(report_file, "w", encoding="utf-8") as handle:
                handle.write(body)
            print(f"Migration report written to {report_file!r}")
        except Exception as write_exc:
            print(f"Failed to write migration report {report_file!r}: {write_exc}", file=sys.stderr)
    print(f"Successful collections: {len(report['successful'])}, Failed collections: {len(report['failed'])}")


def add_common_global_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--debug", action="store_true", default=False)
    parser.add_argument("--trace", action="store_true", default=False)
    parser.add_argument("--skip-tls-verification", action="store_true", default=False)


def add_common_migration_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--migration.batch-size", dest="migration_batch_size", type=int)
    parser.add_argument("--migration.restart", dest="migration_restart", action="store_true", default=None)
    parser.add_argument("--no-migration.restart", dest="migration_restart", action="store_false")
    parser.add_argument(
        "--migration.create-collection",
        dest="migration_create_collection",
        action="store_true",
        default=None,
    )
    parser.add_argument(
        "--no-migration.create-collection",
        dest="migration_create_collection",
        action="store_false",
    )
    parser.add_argument(
        "--migration.offsets-collection",
        dest="migration_offsets_collection",
    )
    parser.add_argument("--migration.batch-delay", dest="migration_batch_delay", type=int)
    parser.add_argument(
        "--qdrant.distance-metric",
        dest="qdrant_distance_metric",
        action="append",
        default=[],
        help="Repeat with name=metric",
    )


def add_single_collection_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--milvus.url", dest="milvus_url")
    parser.add_argument("--milvus.collection", dest="milvus_collection")
    parser.add_argument("--milvus.api-key", dest="milvus_api_key")
    parser.add_argument("--milvus.enable-tls-auth", dest="milvus_enable_tls_auth", action="store_true", default=None)
    parser.add_argument("--no-milvus.enable-tls-auth", dest="milvus_enable_tls_auth", action="store_false")
    parser.add_argument("--milvus.username", dest="milvus_username")
    parser.add_argument("--milvus.password", dest="milvus_password")
    parser.add_argument("--milvus.db-name", dest="milvus_db_name")
    parser.add_argument("--milvus.server-version", dest="milvus_server_version")
    parser.add_argument("--milvus.partitions", dest="milvus_partitions", action="append")

    parser.add_argument("--qdrant.url", dest="qdrant_url")
    parser.add_argument("--qdrant.api-key", dest="qdrant_api_key")
    parser.add_argument("--qdrant.collection", dest="qdrant_collection")


def add_database_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--milvus.url", dest="milvus_url")
    parser.add_argument("--milvus.api-key", dest="milvus_api_key")
    parser.add_argument("--milvus.enable-tls-auth", dest="milvus_enable_tls_auth", action="store_true", default=None)
    parser.add_argument("--no-milvus.enable-tls-auth", dest="milvus_enable_tls_auth", action="store_false")
    parser.add_argument("--milvus.username", dest="milvus_username")
    parser.add_argument("--milvus.password", dest="milvus_password")
    parser.add_argument("--milvus.db-name", dest="milvus_db_name")
    parser.add_argument("--milvus.server-version", dest="milvus_server_version")
    parser.add_argument("--milvus.partitions", dest="milvus_partitions", action="append")

    parser.add_argument("--qdrant.url", dest="qdrant_url")
    parser.add_argument("--qdrant.api-key", dest="qdrant_api_key")
    parser.add_argument("--qdrant.collection-prefix", dest="qdrant_collection_prefix")
    parser.add_argument("--report-file", dest="report_file")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Migrate data from Milvus to Qdrant.")

    subparsers = parser.add_subparsers(dest="command", required=True)

    milvus_parser = subparsers.add_parser("milvus", help="Migrate a Milvus collection to Qdrant.")
    add_common_global_arguments(milvus_parser)
    add_single_collection_arguments(milvus_parser)
    add_common_migration_arguments(milvus_parser)

    milvus_db_parser = subparsers.add_parser("milvus-db", help="Migrate all Milvus collections in a database to Qdrant.")
    add_common_global_arguments(milvus_db_parser)
    add_database_arguments(milvus_db_parser)
    add_common_migration_arguments(milvus_db_parser)

    return parser


def args_to_globals(args: argparse.Namespace) -> Globals:
    return Globals(
        debug=bool(args.debug),
        trace=bool(args.trace),
        skip_tls_verification=bool(args.skip_tls_verification),
    )


def args_to_migration(args: argparse.Namespace) -> MigrationConfig:
    return MigrationConfig(
        batch_size=args.migration_batch_size,
        restart=bool(args.migration_restart),
        create_collection=bool(args.migration_create_collection),
        offsets_collection=args.migration_offsets_collection,
        batch_delay_ms=args.migration_batch_delay,
    )


def config_to_migration(args: argparse.Namespace, config: dict[str, Any]) -> MigrationConfig:
    batch_size = get_arg_or_config(args, "migration_batch_size", config, "migration", "batch_size", default=50)
    restart = bool(get_arg_or_config(args, "migration_restart", config, "migration", "restart", default=False))
    create_collection = bool(
        get_arg_or_config(args, "migration_create_collection", config, "migration", "create_collection", default=True)
    )
    offsets_collection = get_arg_or_config(
        args,
        "migration_offsets_collection",
        config,
        "migration",
        "offsets_collection",
        default=DEFAULT_OFFSETS_COLLECTION,
    )
    batch_delay = get_arg_or_config(args, "migration_batch_delay", config, "migration", "batch_delay", default=0)
    return MigrationConfig(
        batch_size=int(batch_size),
        restart=restart,
        create_collection=create_collection,
        offsets_collection=str(offsets_collection),
        batch_delay_ms=int(batch_delay),
    )


def get_required(value: Any, name: str) -> Any:
    if value is None or value == "":
        raise MigrationError(f"missing required config value: {name}")
    return value


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    config = load_config()

    globals_cfg = args_to_globals(args)
    migration_cfg = config_to_migration(args, config)
    configured_distance_metrics = deep_get(config, "qdrant", "distance_metric")
    if configured_distance_metrics is None:
        configured_distance_metrics = {}
    if not isinstance(configured_distance_metrics, dict):
        raise MigrationError("config qdrant.distance_metric must be an object")
    distance_metrics = {str(k): str(v) for k, v in configured_distance_metrics.items()}
    distance_metrics.update(parse_distance_metrics(args.qdrant_distance_metric))

    try:
        if args.command == "milvus":
            migrate_collection(
                globals_cfg=globals_cfg,
                milvus_config=MilvusConfig(
                    url=str(get_required(get_arg_or_config(args, "milvus_url", config, "milvus", "url"), "milvus.url")),
                    collection=str(
                        get_required(get_arg_or_config(args, "milvus_collection", config, "milvus", "collection"), "milvus.collection")
                    ),
                    api_key=get_arg_or_config(args, "milvus_api_key", config, "milvus", "api_key"),
                    enable_tls_auth=bool(
                        get_arg_or_config(args, "milvus_enable_tls_auth", config, "milvus", "enable_tls_auth", default=False)
                    ),
                    username=get_arg_or_config(args, "milvus_username", config, "milvus", "username"),
                    password=get_arg_or_config(args, "milvus_password", config, "milvus", "password"),
                    db_name=get_arg_or_config(args, "milvus_db_name", config, "milvus", "db_name"),
                    server_version=get_arg_or_config(args, "milvus_server_version", config, "milvus", "server_version"),
                    partitions=list(get_arg_or_config(args, "milvus_partitions", config, "milvus", "partitions", default=[])),
                ),
                qdrant_config=QdrantConfig(
                    url=str(get_required(get_arg_or_config(args, "qdrant_url", config, "qdrant", "url"), "qdrant.url")),
                    api_key=get_arg_or_config(args, "qdrant_api_key", config, "qdrant", "api_key"),
                    collection=str(
                        get_required(get_arg_or_config(args, "qdrant_collection", config, "qdrant", "collection"), "qdrant.collection")
                    ),
                ),
                migration_config=migration_cfg,
                distance_metric_overrides=distance_metrics,
            )
            return 0

        if args.command == "milvus-db":
            migrate_database(
                globals_cfg=globals_cfg,
                milvus_config=MilvusDBConfig(
                    url=str(get_required(get_arg_or_config(args, "milvus_url", config, "milvus", "url"), "milvus.url")),
                    api_key=get_arg_or_config(args, "milvus_api_key", config, "milvus", "api_key"),
                    enable_tls_auth=bool(
                        get_arg_or_config(args, "milvus_enable_tls_auth", config, "milvus", "enable_tls_auth", default=False)
                    ),
                    username=get_arg_or_config(args, "milvus_username", config, "milvus", "username"),
                    password=get_arg_or_config(args, "milvus_password", config, "milvus", "password"),
                    db_name=get_required(get_arg_or_config(args, "milvus_db_name", config, "milvus", "db_name"), "milvus.db_name"),
                    server_version=get_arg_or_config(args, "milvus_server_version", config, "milvus", "server_version"),
                    partitions=list(get_arg_or_config(args, "milvus_partitions", config, "milvus", "partitions", default=[])),
                ),
                qdrant_config=QdrantTargetConfig(
                    url=str(get_required(get_arg_or_config(args, "qdrant_url", config, "qdrant", "url"), "qdrant.url")),
                    api_key=get_arg_or_config(args, "qdrant_api_key", config, "qdrant", "api_key"),
                ),
                migration_config=migration_cfg,
                distance_metric_overrides=distance_metrics,
                collection_prefix=str(
                    get_arg_or_config(args, "qdrant_collection_prefix", config, "qdrant", "collection_prefix", default="")
                ),
                report_file=args.report_file,
            )
            return 0
    except Exception as exc:
        print(exc, file=sys.stderr)
        return 1

    parser.print_help()
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
