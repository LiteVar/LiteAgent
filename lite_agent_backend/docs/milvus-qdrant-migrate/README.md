# ⛟ Milvus -> Qdrant Migrator

This directory contains a Python implementation of the Milvus migration flow.

The script requires a `config.yaml` file in the same directory as `milvus_qdrant.py`.

## Usage Order

1. Fill in `config.yaml` first.
2. Decide whether to migrate a single collection or the whole database.
3. Run the corresponding command.

Scope:

- `milvus`: migrate a single Milvus collection to a Qdrant collection
- `milvus-db`: list all collections in a Milvus database and migrate them one by one

## Config

The script reads `config.yaml` from the same directory as `milvus_qdrant.py`.

Minimal `config.yaml`:

```yaml
milvus:
  url: "http://127.0.0.1:19530"
  username: "root"
  password: "Milvus"
  db_name: "default"
  enable_tls_auth: false

qdrant:
  url: "http://127.0.0.1:6334"
  api_key: "1234567890"
  collection_prefix: ""

migration:
  batch_size: 50
  restart: false
  create_collection: true
  offsets_collection: "_migration_offsets"
  batch_delay: 0
```

Notes:

- `milvus.collection` is used by single-collection migration
- `qdrant.collection` is used by single-collection migration
- `qdrant.collection_prefix` is used by whole-database migration
- `qdrant.url` must point to the Qdrant gRPC endpoint, consistent with the Go implementation
- `qdrant.distance_metric` can optionally override the distance of individual vector fields

## Commands

Migrate a single collection:

```bash
python milvus_qdrant.py milvus
```

Migrate the whole database:

```bash
python milvus_qdrant.py milvus-db
```

## Running Environment

If the target machine already has a usable Python environment, run it directly from this directory:

```bash
pip install -r requirements.txt
python milvus_qdrant.py milvus
python milvus_qdrant.py milvus-db
```

If the target machine does not have Python but does have Docker, copy this whole directory to that machine and run it in a container.

Example:

```bash
docker run --rm -it \
  -v "$PWD":/app \
  milvus-qdrant-py milvus-db
```

Recommended workflow:

- build the image once to install dependencies
- during development/debugging, mount the whole current directory to `/app`
- this makes `config.yaml` and `milvus_qdrant.py` use the latest local files without rebuilding the image every time

Example build:

```bash
docker build -t milvus-qdrant-py .
```

This keeps the tool self-contained inside the current folder while still reusing the dependencies already baked into the image.
