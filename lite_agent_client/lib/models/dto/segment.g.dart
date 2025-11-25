// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'segment.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

SegmentDto _$SegmentDtoFromJson(Map<String, dynamic> json) => SegmentDto(
      json['id'] as String? ?? '',
      json['datasetId'] as String? ?? '',
      json['documentId'] as String? ?? '',
      json['content'] as String? ?? '',
      (json['wordCount'] as num?)?.toInt() ?? 0,
      (json['tokenCount'] as num?)?.toInt() ?? 0,
      (json['hitCount'] as num?)?.toInt() ?? 0,
      json['enableFlag'] as bool? ?? true,
      json['createTime'] as String? ?? '',
      (json['score'] as num?)?.toDouble(),
    );

Map<String, dynamic> _$SegmentDtoToJson(SegmentDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'datasetId': instance.datasetId,
      'documentId': instance.documentId,
      'content': instance.content,
      'wordCount': instance.wordCount,
      'tokenCount': instance.tokenCount,
      'hitCount': instance.hitCount,
      'enableFlag': instance.enableFlag,
      'createTime': instance.createTime,
      'score': instance.score,
    };
