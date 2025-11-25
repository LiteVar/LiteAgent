// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModelDTO _$ModelDTOFromJson(Map<String, dynamic> json) => ModelDTO(
      json['id'] as String? ?? '',
      json['alias'] as String? ?? '',
      json['name'] as String? ?? '',
      json['baseUrl'] as String? ?? '',
      json['apiKey'] as String? ?? '',
      (json['maxTokens'] as num?)?.toInt() ?? 4096,
      json['type'] as String? ?? 'LLM',
      json['autoAgent'] as bool? ?? false,
      json['toolInvoke'] as bool? ?? false,
      json['deepThink'] as bool? ?? false,
      json['similarId'] as String? ?? '',
      (json['operate'] as num?)?.toInt() ?? 0,
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
      'similarId': instance.similarId,
      'operate': instance.operate,
    };
