// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModelDTO _$ModelDTOFromJson(Map<String, dynamic> json) => ModelDTO(
      json['id'] as String,
      json['alias'] as String,
      json['name'] as String,
      json['baseUrl'] as String,
      json['apiKey'] as String,
      (json['maxTokens'] as num?)?.toInt(),
      json['type'] as String,
      json['autoAgent'] as bool?,
      json['toolInvoke'] as bool?,
      json['deepThink'] as bool?,
    );

Map<String, dynamic> _$ModelDTOToJson(ModelDTO instance) => <String, dynamic>{
      'id': instance.id,
      'alias': instance.alias,
      'name': instance.name,
      'baseUrl': instance.baseUrl,
      'apiKey': instance.apiKey,
      'maxTokens': instance.maxTokens,
      'type': instance.type,
      'autoAgent': instance.autoAgent,
      'toolInvoke': instance.toolInvoke,
      'deepThink': instance.deepThink,
    };
