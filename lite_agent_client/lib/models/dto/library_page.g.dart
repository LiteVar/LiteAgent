// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'library_page.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LibraryPageDto _$LibraryPageDtoFromJson(Map<String, dynamic> json) =>
    LibraryPageDto(
      (json['pageNo'] as num).toInt(),
      (json['pageSize'] as num).toInt(),
      json['total'] as String,
      (json['list'] as List<dynamic>)
          .map((e) => LibraryDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$LibraryPageDtoToJson(LibraryPageDto instance) =>
    <String, dynamic>{
      'pageNo': instance.pageNo,
      'pageSize': instance.pageSize,
      'total': instance.total,
      'list': instance.list,
    };
