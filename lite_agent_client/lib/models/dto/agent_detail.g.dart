// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'agent_detail.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AgentDetailDTO _$AgentDetailDTOFromJson(Map<String, dynamic> json) =>
    AgentDetailDTO(
      AgentDTO.fromJson(json['agent'] as Map<String, dynamic>),
      json['model'] == null
          ? null
          : ModelDTO.fromJson(json['model'] as Map<String, dynamic>),
      (json['toolList'] as List<dynamic>?)
          ?.map((e) => ToolDTO.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$AgentDetailDTOToJson(AgentDetailDTO instance) =>
    <String, dynamic>{
      'agent': instance.agent,
      'model': instance.model,
      'toolList': instance.toolList,
    };
