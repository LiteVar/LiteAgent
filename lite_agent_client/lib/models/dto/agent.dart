import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/function.dart';

part 'agent.g.dart';

List<AgentDTO> getAgentDTOList(List<dynamic> list) {
  List<AgentDTO> result = [];
  for (var item in list) {
    result.add(AgentDTO.fromJson(item));
  }
  return result;
}

@JsonSerializable()
class AgentDTO extends Object {
  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'name')
  String? name;

  @JsonKey(name: 'icon')
  String? icon;

  @JsonKey(name: 'description')
  String? description;

  @JsonKey(name: 'prompt')
  String? prompt;

  @JsonKey(name: 'llmModelId')
  String? llmModelId;

  @JsonKey(name: 'shareTip')
  bool? shareTip;

  @JsonKey(name: 'temperature')
  double? temperature;

  @JsonKey(name: 'topP')
  double? topP;

  @JsonKey(name: 'maxTokens')
  int? maxTokens;

  @JsonKey(name: 'createTime')
  String? createTime;

  @JsonKey(name: 'toolFunctionList')
  List<FunctionDto>? toolFunctionList;

  @JsonKey(name: 'subAgentIds')
  List<String>? subAgentIds;

  @JsonKey(name: 'type')
  int? type;

  @JsonKey(name: 'mode')
  int? mode;

  @JsonKey(name: 'datasetIds')
  List<String>? datasetIds;

  @JsonKey(name: 'ttsModelId')
  String? ttsModelId;

  @JsonKey(name: 'asrModelId')
  String? asrModelId;

  @JsonKey(name: 'autoAgentFlag')
  bool? autoAgentFlag;

  bool? isCloud = false; // just for local data flag

  AgentDTO(
    this.id,
    this.name,
    this.icon,
    this.description,
    this.prompt,
    this.llmModelId,
    this.shareTip,
    this.temperature,
    this.topP,
    this.maxTokens,
    this.createTime,
    this.toolFunctionList,
    this.subAgentIds,
    this.type,
    this.mode,
    this.datasetIds,
    this.isCloud,
    this.autoAgentFlag,
    this.ttsModelId,
    this.asrModelId,
  );

  factory AgentDTO.fromJson(Map<String, dynamic> srcJson) => _$AgentDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AgentDTOToJson(this);
}
