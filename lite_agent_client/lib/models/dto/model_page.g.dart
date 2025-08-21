// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'model_page.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModelPageDto _$ModelPageDtoFromJson(Map<String, dynamic> json) => ModelPageDto(
      (json['pageNo'] as num).toInt(),
      (json['pageSize'] as num).toInt(),
      json['total'] as String,
      (json['list'] as List<dynamic>)
          .map((e) => ModelDTO.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$ModelPageDtoToJson(ModelPageDto instance) =>
    <String, dynamic>{
      'pageNo': instance.pageNo,
      'pageSize': instance.pageSize,
      'total': instance.total,
      'list': instance.list,
    };
