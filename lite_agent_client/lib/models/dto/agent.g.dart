// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'agent.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AgentDTO _$AgentDTOFromJson(Map<String, dynamic> json) => AgentDTO(
      json['id'] as String,
      json['userId'] as String?,
      json['workspaceId'] as String?,
      json['name'] as String?,
      json['icon'] as String?,
      json['description'] as String?,
      json['prompt'] as String?,
      json['llmModelId'] as String?,
      (json['toolIds'] as List<dynamic>?)?.map((e) => e as String).toList(),
      (json['status'] as num?)?.toInt(),
      json['shareTip'] as bool?,
      (json['temperature'] as num?)?.toDouble(),
      (json['topP'] as num?)?.toDouble(),
      (json['maxTokens'] as num?)?.toInt(),
      json['createTime'] as String?,
      json['updateTime'] as String?,
      (json['toolFunctionList'] as List<dynamic>?)
          ?.map((e) => FunctionDto.fromJson(e as Map<String, dynamic>))
          .toList(),
      (json['subAgentIds'] as List<dynamic>?)?.map((e) => e as String).toList(),
      (json['type'] as num?)?.toInt(),
      (json['mode'] as num?)?.toInt(),
      (json['datasetIds'] as List<dynamic>?)?.map((e) => e as String).toList(),
      json['isCloud'] as bool?,
    );

Map<String, dynamic> _$AgentDTOToJson(AgentDTO instance) => <String, dynamic>{
      'id': instance.id,
      'userId': instance.userId,
      'workspaceId': instance.workspaceId,
      'name': instance.name,
      'icon': instance.icon,
      'description': instance.description,
      'prompt': instance.prompt,
      'llmModelId': instance.llmModelId,
      'toolIds': instance.toolIds,
      'status': instance.status,
      'shareTip': instance.shareTip,
      'temperature': instance.temperature,
      'topP': instance.topP,
      'maxTokens': instance.maxTokens,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
      'toolFunctionList': instance.toolFunctionList,
      'subAgentIds': instance.subAgentIds,
      'type': instance.type,
      'mode': instance.mode,
      'datasetIds': instance.datasetIds,
      'isCloud': instance.isCloud,
    };
