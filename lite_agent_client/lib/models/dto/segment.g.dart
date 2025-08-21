// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'segment.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

SegmentDto _$SegmentDtoFromJson(Map<String, dynamic> json) => SegmentDto(
      json['id'] as String,
      json['userId'] as String,
      json['workspaceId'] as String,
      json['datasetId'] as String,
      json['documentId'] as String,
      json['embeddingId'] as String,
      json['vectorCollectionName'] as String,
      json['content'] as String,
      json['metadata'] as String,
      (json['wordCount'] as num).toInt(),
      (json['tokenCount'] as num).toInt(),
      (json['hitCount'] as num).toInt(),
      json['enableFlag'] as bool,
      json['createTime'] as String,
      (json['score'] as num?)?.toDouble(),
    );

Map<String, dynamic> _$SegmentDtoToJson(SegmentDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'userId': instance.userId,
      'workspaceId': instance.workspaceId,
      'datasetId': instance.datasetId,
      'documentId': instance.documentId,
      'embeddingId': instance.embeddingId,
      'vectorCollectionName': instance.vectorCollectionName,
      'content': instance.content,
      'metadata': instance.metadata,
      'wordCount': instance.wordCount,
      'tokenCount': instance.tokenCount,
      'hitCount': instance.hitCount,
      'enableFlag': instance.enableFlag,
      'createTime': instance.createTime,
      'score': instance.score,
    };
