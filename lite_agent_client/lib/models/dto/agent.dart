import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/function.dart';

part 'agent.g.dart';

@JsonSerializable()
class AgentDTO extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'name', defaultValue: '')
  String name;

  @JsonKey(name: 'icon', defaultValue: '')
  String icon;

  @JsonKey(name: 'description', defaultValue: '')
  String description;

  @JsonKey(name: 'prompt', defaultValue: '')
  String prompt;

  @JsonKey(name: 'llmModelId', defaultValue: '')
  String llmModelId;

  @JsonKey(name: 'shareTip', defaultValue: false)
  bool shareTip;

  @JsonKey(name: 'temperature', defaultValue: 0.0)
  double temperature;

  @JsonKey(name: 'topP', defaultValue: 1.0)
  double topP;

  @JsonKey(name: 'maxTokens', defaultValue: 4096)
  int maxTokens;

  @JsonKey(name: 'toolFunctionList')
  List<FunctionDto>? toolFunctionList;

  @JsonKey(name: 'subAgentIds', defaultValue: [])
  List<String> subAgentIds;

  @JsonKey(name: 'type', defaultValue: 0)
  int type;

  @JsonKey(name: 'mode', defaultValue: 0)
  int mode;

  @JsonKey(name: 'datasetIds')
  List<String>? datasetIds;

  @JsonKey(name: 'ttsModelId', defaultValue: '')
  String ttsModelId;

  @JsonKey(name: 'asrModelId', defaultValue: '')
  String asrModelId;

  @JsonKey(name: 'autoAgentFlag', defaultValue: false)
  bool autoAgentFlag;

  ///是否为云端模型(仅用于本地数据标识)
  bool? isCloud = false;

  ///导入时发现的重名模型id
  String? similarId;

  ///导入时对重名模型的操作
  int? operate;

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
    this.toolFunctionList,
    this.subAgentIds,
    this.type,
    this.mode,
    this.datasetIds,
    this.isCloud,
    this.autoAgentFlag,
    this.ttsModelId,
    this.asrModelId,
    this.similarId,
    this.operate,
  );

  factory AgentDTO.fromJson(Map<String, dynamic> srcJson) => _$AgentDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AgentDTOToJson(this);
}
