// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'retrieval_result.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RetrievalResultDto _$RetrievalResultDtoFromJson(Map<String, dynamic> json) =>
    RetrievalResultDto(
      json['id'] as String? ?? '',
      json['content'] as String? ?? '',
      (json['tokenCount'] as num?)?.toInt() ?? 0,
      json['datasetId'] as String? ?? '',
      json['documentId'] as String? ?? '',
      json['documentName'] as String? ?? '',
      (json['score'] as num?)?.toDouble() ?? 0.0,
      json['fileId'] as String? ?? '',
    );

Map<String, dynamic> _$RetrievalResultDtoToJson(RetrievalResultDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'content': instance.content,
      'tokenCount': instance.tokenCount,
      'datasetId': instance.datasetId,
      'documentId': instance.documentId,
      'documentName': instance.documentName,
      'score': instance.score,
      'fileId': instance.fileId,
    };
