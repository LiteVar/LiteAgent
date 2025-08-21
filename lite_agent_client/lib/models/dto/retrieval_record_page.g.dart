// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'retrieval_record_page.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RetrievalRecordPageDto _$RetrievalRecordPageDtoFromJson(
        Map<String, dynamic> json) =>
    RetrievalRecordPageDto(
      (json['pageNo'] as num).toInt(),
      (json['pageSize'] as num).toInt(),
      json['total'] as String,
      (json['list'] as List<dynamic>)
          .map((e) => RetrievalRecordDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$RetrievalRecordPageDtoToJson(
        RetrievalRecordPageDto instance) =>
    <String, dynamic>{
      'pageNo': instance.pageNo,
      'pageSize': instance.pageSize,
      'total': instance.total,
      'list': instance.list,
    };
