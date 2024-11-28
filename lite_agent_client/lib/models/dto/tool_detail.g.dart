// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tool_detail.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ToolDetailDTO _$ToolDetailDTOFromJson(Map<String, dynamic> json) =>
    ToolDetailDTO(
      json['id'] as String,
      json['userId'] as String?,
      json['workspaceId'] as String?,
      json['name'] as String?,
      json['description'] as String?,
      (json['schemaType'] as num?)?.toInt(),
      json['schemaStr'] as String?,
      json['apiKeyType'] as String?,
      json['shareFlag'] as bool?,
      json['createTime'] as String,
      json['updateTime'] as String,
    )..apiKey = json['apiKey'] as String?;

Map<String, dynamic> _$ToolDetailDTOToJson(ToolDetailDTO instance) =>
    <String, dynamic>{
      'id': instance.id,
      'userId': instance.userId,
      'workspaceId': instance.workspaceId,
      'name': instance.name,
      'description': instance.description,
      'schemaType': instance.schemaType,
      'schemaStr': instance.schemaStr,
      'apiKeyType': instance.apiKeyType,
      'apiKey': instance.apiKey,
      'shareFlag': instance.shareFlag,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
