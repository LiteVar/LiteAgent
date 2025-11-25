// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tool.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ToolDTO _$ToolDTOFromJson(Map<String, dynamic> json) => ToolDTO(
      json['id'] as String? ?? '',
      json['name'] as String? ?? '',
      json['description'] as String? ?? '',
      (json['schemaType'] as num?)?.toInt() ?? 0,
      json['schemaStr'] as String? ?? '',
      json['apiKey'] as String? ?? '',
      json['apiKeyType'] as String? ?? '',
      json['shareFlag'] as bool? ?? false,
      json['autoAgent'] as bool? ?? false,
      (json['functionList'] as List<dynamic>?)
          ?.map((e) => FunctionDto.fromJson(e as Map<String, dynamic>))
          .toList(),
      json['similarId'] as String?,
      (json['operate'] as num?)?.toInt(),
    );

Map<String, dynamic> _$ToolDTOToJson(ToolDTO instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'schemaType': instance.schemaType,
      'schemaStr': instance.schemaStr,
      'apiKey': instance.apiKey,
      'apiKeyType': instance.apiKeyType,
      'shareFlag': instance.shareFlag,
      'autoAgent': instance.autoAgent,
      'functionList': instance.functionList,
      'similarId': instance.similarId,
      'operate': instance.operate,
    };
