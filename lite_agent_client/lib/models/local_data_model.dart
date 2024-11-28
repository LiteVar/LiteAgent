import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

import '../config/constants.dart';

part 'local_data_model.g.dart'; // 自动生成：dart run build_runner build

@HiveType(typeId: HiveTypeIds.messageBeanTypeId)
class ChatMessage {
  @HiveField(0)
  int sendRole = 0;
  @HiveField(1)
  String userName = "";
  @HiveField(2)
  String message = "";
  @HiveField(3)
  String imgFilePath = "";
}

class ChatRole {
  static const int User = 0;
  static const int Agent = 1;
}

@HiveType(typeId: HiveTypeIds.agentConversationBeanTypeId)
class AgentConversationBean {
  @HiveField(0)
  String agentId = "";
  @HiveField(1)
  var chatMessageList = <ChatMessage>[];
  @HiveField(2)
  bool isCloud = false;
  @HiveField(3)
  int? updateTime = 0;

  AgentBean? agent;
}

@HiveType(typeId: HiveTypeIds.modelBeanTypeId)
class ModelBean {
  @HiveField(0)
  String id = "";
  @HiveField(1)
  String name = "";
  @HiveField(2)
  String url = "";
  @HiveField(3)
  String key = "";
}

@HiveType(typeId: HiveTypeIds.toolBeanTypeId)
class ToolBean {
  @HiveField(0)
  String id = "";
  @HiveField(1)
  String name = "";
  @HiveField(2)
  String description = "";
  @HiveField(3)
  String schemaType = "";
  @HiveField(4)
  String schemaText = "";
  @HiveField(5)
  String apiType = "";
  @HiveField(6)
  String apiText = "";
}

@HiveType(typeId: HiveTypeIds.agentBeanTypeId)
class AgentBean {
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
  List<String> toolList = <String>[];
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

  AgentBean(
      {this.id = "",
      this.name = "",
      this.iconPath = "",
      this.description = "",
      this.modelId = "",
      this.toolList = const <String>[],
      this.prompt = "",
      this.temperature = 0.0,
      this.maxToken = 4096,
      this.topP = 1.0});

  AgentDTO translateToDTO() {
    return AgentDTO(id, "", "", name, iconPath, description, prompt, modelId, toolList, 0, false, temperature, topP, maxToken, "", "");
  }

  void translateFromDTO(AgentDTO agent) {
    this.id = agent.id;
    this.name = agent.name ?? "";
    this.iconPath = agent.icon ?? "";
    this.description = agent.description ?? "";
    this.modelId = agent.llmModelId ?? "";
    this.toolList = agent.toolIds ?? <String>[];
    this.prompt = agent.prompt ?? "";
    this.temperature = agent.temperature ?? 0.0;
    this.maxToken = agent.maxTokens ?? 4096;
    this.topP = agent.topP ?? 0.0;
    this.isCloud = !agent.id.isNumeric();
  }
}
