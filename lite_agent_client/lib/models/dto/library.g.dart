// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'library.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LibraryDto _$LibraryDtoFromJson(Map<String, dynamic> json) => LibraryDto(
      json['id'] as String,
      json['name'] as String,
      json['userId'] as String?,
      json['workspaceId'] as String?,
      json['icon'] as String?,
      json['description'] as String?,
      json['shareFlag'] as bool?,
      json['dataSourceType'] as String?,
      json['llmModelId'] as String?,
      json['embeddingModel'] as String?,
      json['embeddingModelProvider'] as String?,
      (json['retrievalTopK'] as num?)?.toInt(),
      (json['retrievalScoreThreshold'] as num?)?.toInt(),
      json['createTime'] as String?,
      json['updateTime'] as String?,
    )
      ..docCount = (json['docCount'] as num?)?.toInt()
      ..wordCount = (json['wordCount'] as num?)?.toInt()
      ..agentCount = (json['agentCount'] as num?)?.toInt();

Map<String, dynamic> _$LibraryDtoToJson(LibraryDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'userId': instance.userId,
      'workspaceId': instance.workspaceId,
      'icon': instance.icon,
      'description': instance.description,
      'shareFlag': instance.shareFlag,
      'dataSourceType': instance.dataSourceType,
      'llmModelId': instance.llmModelId,
      'embeddingModel': instance.embeddingModel,
      'embeddingModelProvider': instance.embeddingModelProvider,
      'retrievalTopK': instance.retrievalTopK,
      'retrievalScoreThreshold': instance.retrievalScoreThreshold,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
      'docCount': instance.docCount,
      'wordCount': instance.wordCount,
      'agentCount': instance.agentCount,
    };
