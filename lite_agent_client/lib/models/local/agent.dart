import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../config/constants.dart';
import 'function.dart';

part 'agent.g.dart';

@JsonSerializable()
@HiveType(typeId: HiveTypeIds.agentModelTypeId)
class AgentModel extends Object{
  @HiveField(0)
  String id = "";
  @HiveField(1)
  String name = "";
  @HiveField(2)
  String iconPath = "";
  @HiveField(3)
  String description = "";
  @HiveField(4)
  String modelId = "";
  @HiveField(5)
  @Deprecated("deprecated in 0.2.0,use functionList")
  List<String>? toolList;
  @HiveField(6)
  String prompt = "";
  @HiveField(7)
  double temperature = 0.0;
  @HiveField(8)
  int maxToken = 4096;
  @HiveField(9)
  double topP = 1.0;
  @HiveField(10)
  bool? isCloud = false;
  @HiveField(11)
  int? createTime = 0;
  @HiveField(12)
  List<ToolFunctionModel>? functionList;
  @HiveField(13)
  List<String>? libraryIds;
  @HiveField(14)
  int? operationMode = 0;
  @HiveField(15)
  int? agentType = 0;
  @HiveField(16)
  List<String>? childAgentIds;
  @HiveField(17)
  int? toolOperationMode = 0;
  @HiveField(18)
  String? ttsModelId = "";
  @HiveField(19)
  String? asrModelId = "";
  @HiveField(20)
  bool? autoAgentFlag = false;

  bool shareFlag = false;

  AgentModel({
    required this.id,
    required this.name,
    required this.iconPath,
    required this.description,
    this.modelId = "",
    this.prompt = "",
    this.temperature = 0.0,
    this.maxToken = 4096,
    this.topP = 1.0,
    this.isCloud = false,
    this.createTime = 0,
    this.functionList,
    this.libraryIds,
    this.operationMode = 0,
    this.agentType = 0,
    this.childAgentIds,
    this.toolOperationMode = 0,
    this.ttsModelId,
    this.asrModelId,
    this.autoAgentFlag = false,
    this.shareFlag = false,
  });

  factory AgentModel.onlyId(String id) {
    return AgentModel(id: id, name: '', iconPath: '', description: '');
  }

  factory AgentModel.fromJson(Map<String, dynamic> srcJson) => _$AgentModelFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AgentModelToJson(this);
}
