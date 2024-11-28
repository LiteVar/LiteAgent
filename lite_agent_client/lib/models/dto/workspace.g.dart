// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'workspace.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

WorkSpaceDTO _$WorkSpaceDTOFromJson(Map<String, dynamic> json) => WorkSpaceDTO(
      json['id'] as String,
      json['name'] as String,
      (json['role'] as num).toInt(),
    );

Map<String, dynamic> _$WorkSpaceDTOToJson(WorkSpaceDTO instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'role': instance.role,
    };
