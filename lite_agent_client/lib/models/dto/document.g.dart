// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'document.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DocumentDto _$DocumentDtoFromJson(Map<String, dynamic> json) => DocumentDto(
      json['id'] as String,
      json['name'] as String,
      json['userId'] as String,
      json['workspaceId'] as String,
      json['datasetId'] as String?,
      json['dataSourceType'] as String?,
      json['filePath'] as String?,
      json['content'] as String?,
      (json['htmlUrl'] as List<dynamic>?)?.map((e) => e as String).toList(),
      json['md5Hash'] as String?,
      (json['wordCount'] as num?)?.toInt(),
      (json['tokenCount'] as num?)?.toInt(),
      json['metadata'] as String?,
      json['enableFlag'] as bool?,
      json['createTime'] as String?,
      json['updateTime'] as String?,
    );

Map<String, dynamic> _$DocumentDtoToJson(DocumentDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'userId': instance.userId,
      'workspaceId': instance.workspaceId,
      'datasetId': instance.datasetId,
      'dataSourceType': instance.dataSourceType,
      'filePath': instance.filePath,
      'content': instance.content,
      'htmlUrl': instance.htmlUrl,
      'md5Hash': instance.md5Hash,
      'wordCount': instance.wordCount,
      'tokenCount': instance.tokenCount,
      'metadata': instance.metadata,
      'enableFlag': instance.enableFlag,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
