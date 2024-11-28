import 'package:json_annotation/json_annotation.dart';

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

  @JsonKey(name: 'userId')
  String? userId;

  @JsonKey(name: 'workspaceId')
  String? workspaceId;

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

  @JsonKey(name: 'toolIds')
  List<String>? toolIds;

  @JsonKey(name: 'status')
  int? status;

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

  @JsonKey(name: 'updateTime')
  String? updateTime;

  AgentDTO(
    this.id,
    this.userId,
    this.workspaceId,
    this.name,
    this.icon,
    this.description,
    this.prompt,
    this.llmModelId,
    this.toolIds,
    this.status,
    this.shareTip,
    this.temperature,
    this.topP,
    this.maxTokens,
    this.createTime,
    this.updateTime,
  );

  factory AgentDTO.fromJson(Map<String, dynamic> srcJson) => _$AgentDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AgentDTOToJson(this);
}
