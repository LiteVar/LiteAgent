// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'agent_detail.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AgentDetailDTO _$AgentDetailDTOFromJson(Map<String, dynamic> json) =>
    AgentDetailDTO(
      json['agent'] == null
          ? null
          : AgentDTO.fromJson(json['agent'] as Map<String, dynamic>),
      json['model'] == null
          ? null
          : ModelDTO.fromJson(json['model'] as Map<String, dynamic>),
      (json['functionList'] as List<dynamic>?)
          ?.map((e) => FunctionDto.fromJson(e as Map<String, dynamic>))
          .toList(),
      (json['datasetList'] as List<dynamic>?)
          ?.map((e) => LibraryDto.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

Map<String, dynamic> _$AgentDetailDTOToJson(AgentDetailDTO instance) =>
    <String, dynamic>{
      'agent': instance.agent,
      'model': instance.model,
      'functionList': instance.functionList,
      'datasetList': instance.datasetList,
    };
