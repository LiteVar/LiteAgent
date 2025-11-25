// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'retrieval_history.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RetrievalHistoryDto _$RetrievalHistoryDtoFromJson(Map<String, dynamic> json) =>
    RetrievalHistoryDto(
      json['id'] as String? ?? '',
      json['datasetId'] as String? ?? '',
      json['datasetName'] as String? ?? '',
    );

Map<String, dynamic> _$RetrievalHistoryDtoToJson(
        RetrievalHistoryDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'datasetId': instance.datasetId,
      'datasetName': instance.datasetName,
    };
