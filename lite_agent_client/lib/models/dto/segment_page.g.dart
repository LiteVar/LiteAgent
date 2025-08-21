// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'segment_page.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

SegmentPageDto _$SegmentPageDtoFromJson(Map<String, dynamic> json) =>
    SegmentPageDto(
      (json['pageNo'] as num).toInt(),
      (json['pageSize'] as num).toInt(),
      json['total'] as String,
      (json['list'] as List<dynamic>)
          .map((e) => SegmentDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$SegmentPageDtoToJson(SegmentPageDto instance) =>
    <String, dynamic>{
      'pageNo': instance.pageNo,
      'pageSize': instance.pageSize,
      'total': instance.total,
      'list': instance.list,
    };
