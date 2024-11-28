import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/tool.dart';

import 'agent.dart';
import 'model.dart';

part 'agent_detail.g.dart';

@JsonSerializable()
class AgentDetailDTO extends Object {
  @JsonKey(name: 'agent')
  AgentDTO agent;

  @JsonKey(name: 'model')
  ModelDTO? model;

  @JsonKey(name: 'toolList')
  List<ToolDTO>? toolList;

  AgentDetailDTO(
    this.agent,
    this.model,
    this.toolList,
  );

  factory AgentDetailDTO.fromJson(Map<String, dynamic> srcJson) => _$AgentDetailDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AgentDetailDTOToJson(this);
}
