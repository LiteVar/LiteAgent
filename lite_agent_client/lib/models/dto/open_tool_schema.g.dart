// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'open_tool_schema.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OpenToolSchemaDTO _$OpenToolSchemaDTOFromJson(Map<String, dynamic> json) =>
    OpenToolSchemaDTO(
      json['origin'] as String? ?? '',
      json['apiKey'] as String? ?? '',
      json['serverUrl'] as String? ?? '',
      json['schema'] as String? ?? '',
    );

Map<String, dynamic> _$OpenToolSchemaDTOToJson(OpenToolSchemaDTO instance) =>
    <String, dynamic>{
      'origin': instance.origin,
      'apiKey': instance.apiKey,
      'serverUrl': instance.serverUrl,
      'schema': instance.schema,
    };
