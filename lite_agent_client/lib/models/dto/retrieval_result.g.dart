// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'retrieval_result.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RetrievalResultDto _$RetrievalResultDtoFromJson(Map<String, dynamic> json) =>
    RetrievalResultDto(
      json['id'] as String,
      json['content'] as String?,
      (json['tokenCount'] as num?)?.toInt(),
      json['documentName'] as String?,
      (json['score'] as num?)?.toDouble(),
    );

Map<String, dynamic> _$RetrievalResultDtoToJson(RetrievalResultDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'content': instance.content,
      'tokenCount': instance.tokenCount,
      'documentName': instance.documentName,
      'score': instance.score,
    };
