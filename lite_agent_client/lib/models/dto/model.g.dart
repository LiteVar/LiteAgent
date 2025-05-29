// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModelDTO _$ModelDTOFromJson(Map<String, dynamic> json) => ModelDTO(
      json['id'] as String,
      json['name'] as String,
      json['baseUrl'] as String,
      json['apiKey'] as String,
    );

Map<String, dynamic> _$ModelDTOToJson(ModelDTO instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'baseUrl': instance.baseUrl,
      'apiKey': instance.apiKey,
    };
