// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tool.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ToolDTO _$ToolDTOFromJson(Map<String, dynamic> json) => ToolDTO(
      json['id'] as String,
      json['userId'] as String?,
      json['workspaceId'] as String?,
      json['name'] as String?,
      json['description'] as String?,
      (json['schemaType'] as num?)?.toInt(),
      json['schemaStr'] as String?,
      json['apiKey'] as String?,
      json['apiKeyType'] as String?,
      json['shareFlag'] as bool?,
      json['createTime'] as String?,
      json['updateTime'] as String?,
    );

Map<String, dynamic> _$ToolDTOToJson(ToolDTO instance) => <String, dynamic>{
      'id': instance.id,
      'userId': instance.userId,
      'workspaceId': instance.workspaceId,
      'name': instance.name,
      'description': instance.description,
      'schemaType': instance.schemaType,
      'schemaStr': instance.schemaStr,
      'apiKey': instance.apiKey,
      'apiKeyType': instance.apiKeyType,
      'shareFlag': instance.shareFlag,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
