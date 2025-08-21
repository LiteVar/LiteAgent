// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'document_page.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DocumentPageDto _$DocumentPageDtoFromJson(Map<String, dynamic> json) =>
    DocumentPageDto(
      (json['pageNo'] as num).toInt(),
      (json['pageSize'] as num).toInt(),
      json['total'] as String,
      (json['list'] as List<dynamic>)
          .map((e) => DocumentDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$DocumentPageDtoToJson(DocumentPageDto instance) =>
    <String, dynamic>{
      'pageNo': instance.pageNo,
      'pageSize': instance.pageSize,
      'total': instance.total,
      'list': instance.list,
    };
