// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'retrieval_record.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RetrievalRecordDto _$RetrievalRecordDtoFromJson(Map<String, dynamic> json) =>
    RetrievalRecordDto(
      json['id'] as String,
      json['datasetId'] as String,
      json['agentId'] as String,
      json['content'] as String,
      json['retrieveType'] as String,
      json['createTime'] as String,
    );

Map<String, dynamic> _$RetrievalRecordDtoToJson(RetrievalRecordDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'datasetId': instance.datasetId,
      'agentId': instance.agentId,
      'content': instance.content,
      'retrieveType': instance.retrieveType,
      'createTime': instance.createTime,
    };
