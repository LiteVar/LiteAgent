import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/function.dart';
import 'package:lite_agent_client/models/dto/library.dart';

import 'agent.dart';
import 'model.dart';

part 'agent_detail.g.dart';

@JsonSerializable()
class AgentDetailDTO extends Object {
  @JsonKey(name: 'agent')
  AgentDTO? agent;

  @JsonKey(name: 'model')
  ModelDTO? model;

  @JsonKey(name: 'ttsModel')
  ModelDTO? ttsModel;

  @JsonKey(name: 'asrModel')
  ModelDTO? asrModel;

  @JsonKey(name: 'functionList')
  List<FunctionDto>? functionList;

  @JsonKey(name: 'datasetList')
  List<LibraryDto>? datasetList;

  AgentDetailDTO(this.agent, this.model, this.functionList, this.datasetList);

  factory AgentDetailDTO.fromJson(Map<String, dynamic> srcJson) => _$AgentDetailDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AgentDetailDTOToJson(this);
}
