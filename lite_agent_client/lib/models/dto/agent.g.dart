// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'agent.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AgentDTO _$AgentDTOFromJson(Map<String, dynamic> json) => AgentDTO(
      json['id'] as String,
      json['name'] as String?,
      json['icon'] as String?,
      json['description'] as String?,
      json['prompt'] as String?,
      json['llmModelId'] as String?,
      json['shareTip'] as bool?,
      (json['temperature'] as num?)?.toDouble(),
      (json['topP'] as num?)?.toDouble(),
      (json['maxTokens'] as num?)?.toInt(),
      json['createTime'] as String?,
      (json['toolFunctionList'] as List<dynamic>?)
          ?.map((e) => FunctionDto.fromJson(e as Map<String, dynamic>))
          .toList(),
      (json['subAgentIds'] as List<dynamic>?)?.map((e) => e as String).toList(),
      (json['type'] as num?)?.toInt(),
      (json['mode'] as num?)?.toInt(),
      (json['datasetIds'] as List<dynamic>?)?.map((e) => e as String).toList(),
      json['isCloud'] as bool?,
      json['autoAgentFlag'] as bool?,
      json['ttsModelId'] as String?,
      json['asrModelId'] as String?,
    );

Map<String, dynamic> _$AgentDTOToJson(AgentDTO instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'icon': instance.icon,
      'description': instance.description,
      'prompt': instance.prompt,
      'llmModelId': instance.llmModelId,
      'shareTip': instance.shareTip,
      'temperature': instance.temperature,
      'topP': instance.topP,
      'maxTokens': instance.maxTokens,
      'createTime': instance.createTime,
      'toolFunctionList': instance.toolFunctionList,
      'subAgentIds': instance.subAgentIds,
      'type': instance.type,
      'mode': instance.mode,
      'datasetIds': instance.datasetIds,
      'ttsModelId': instance.ttsModelId,
      'asrModelId': instance.asrModelId,
      'autoAgentFlag': instance.autoAgentFlag,
      'isCloud': instance.isCloud,
    };
