import 'dart:core';

import 'package:get/get.dart';
import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/function.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';

import '../config/constants.dart';
import '../utils/tool_schema_parser.dart';
import '../widgets/dialog/dialog_tool_edit.dart';

part 'local_data_model.g.dart'; // 自动生成：dart run build_runner build

@HiveType(typeId: HiveTypeIds.messageBeanTypeId)
class ChatMessage {
  @HiveField(0)
  int sendRole = 0;
  @HiveField(1)
  String roleName = "";
  @HiveField(2)
  String message = "";
  @HiveField(3)
  String imgFilePath = "";
  @HiveField(4)
  @Deprecated("deprecated in 0.2.0,use subMessages")
  List<String>? childAgentMessageList;
  @HiveField(5)
  @Deprecated("deprecated in 0.2.0,use roleName")
  String? toolName;
  @HiveField(6)
  @Deprecated("deprecated in 1.0.0,use subMessages")
  List<Thought>? thoughtList;
  @HiveField(7)
  String? taskId = ""; //sometime for MessagesTaskId,sometime for functionId,according to ChatRoleType
  @HiveField(8)
  List<ChatMessage>? subMessages;
  @HiveField(9)
  String? receivedMessage = ""; //only for subMessages

  bool isLoading = false;
  bool isMessageExpanded = true;
}

class ChatRoleType {
  static const int User = 0;
  static const int Agent = 1;
  static const int SubAgent = 2;
  static const int Tool = 3;
  static const int Reflection = 4;
  static const int Reasoning = 5;
}

@Deprecated("deprecated in 1.0.0,use ChatMessage")
@HiveType(typeId: HiveTypeIds.messageThoughtTypeId)
class Thought {
  @HiveField(0)
  String id = "";
  @HiveField(1)
  String type = "";
  @HiveField(2)
  String roleName = "";
  @HiveField(3)
  String sentMessage = "";
  @HiveField(4)
  String receivedMessage = "";
}

@Deprecated("deprecated in 1.0.0,use ChatRoleType")
class ThoughtRoleType {
  static const String Tool = "tool";
  static const String Agent = "agent";
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
  @HiveField(4)
  String? maxToken = "";
  @HiveField(5)
  int? createTime = 0;
  @HiveField(6)
  String? type = "";
  @HiveField(7)
  String? nickName = "";
  @HiveField(8)
  bool? supportMultiAgent;
  @HiveField(9)
  bool? supportToolCalling;
  @HiveField(10)
  bool? supportDeepThinking;
}

class OperationMode {
  static const int PARALLEL = 0;
  static const int SERIAL = 1;
  static const int REJECT = 2;
}

class AgentType {
  static const int GENERAL = 0;
  static const int DISTRIBUTE = 1;
  static const int REFLECTION = 2;
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
  @HiveField(7)
  int? createTime = 0;
  @HiveField(8)
  @Deprecated("deprecated in 1.0.0,use schemaText according to schemaType")
  String? thirdSchemaText = "";
  @HiveField(9)
  @Deprecated("deprecated in 1.0.0,use schemaText according to schemaType")
  String? mcpText = "";
  @HiveField(10)
  bool? supportMultiAgent;

  List<AgentToolFunction> functionList = <AgentToolFunction>[];

  ToolDTO translateToDTO() {
    int schemaType = 0;
    String apiKeyType = "";
    if (this.schemaType == SchemaType.OPENAPI || this.schemaType == Protocol.OPENAPI) {
      schemaType = 1; //openapi
    } else if (this.schemaType == SchemaType.JSONRPCHTTP || this.schemaType == Protocol.JSONRPCHTTP) {
      schemaType = 2; //json rpc
    } else if (this.schemaType == SchemaType.OPENMODBUS || this.schemaType == Protocol.OPENMODBUS) {
      schemaType = 3; //open modbus
    } else if (this.schemaType == SchemaType.OPENTOOL || this.schemaType == Protocol.OPENTOOL) {
      schemaType = 4; //open tool
    }
    if (apiType == "basic" || apiType == "Basic") {
      apiKeyType = "Basic";
    } else if (apiType == "bearer" || apiType == "Bearer") {
      apiKeyType = "Bearer";
    }
    return ToolDTO(
        id, null, null, name, description, schemaType, schemaText, apiText, apiKeyType, false, "", "", supportMultiAgent ?? false, null);
  }

  Future<void> initToolFunctionList({bool showLoading = false}) async {
    functionList = await ToolSchemaParser.parseFunctions(this, showLoading);
  }
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
  List<AgentToolFunction>? functionList;
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

  AgentBean({
    this.id = "",
    this.name = "",
    this.iconPath = "",
    this.description = "",
    this.modelId = "",
    this.toolList,
    this.prompt = "",
    this.temperature = 0.0,
    this.maxToken = 4096,
    this.topP = 1.0,
  });

  AgentDTO translateToDTO() {
    String createTime = "";
    late DateTime dateTime;
    if (this.createTime != null) {
      dateTime = DateTime.fromMicrosecondsSinceEpoch(this.createTime ?? 0);
    } else if (id.isNumericOnly) {
      dateTime = DateTime.fromMicrosecondsSinceEpoch(int.parse(id));
    }
    var dateTimeString = dateTime.toString();
    if (dateTimeString.contains(".")) {
      dateTimeString = (dateTimeString.split("."))[0];
    }
    createTime = dateTimeString;

    var webFunctionList = <FunctionDto>[];
    if (functionList != null) {
      for (var function in functionList!) {
        var dto = function.translateToDTO();
        dto.mode = toolOperationMode ?? 0;
        webFunctionList.add(dto);
      }
    }

    return AgentDTO(id, name, iconPath, description, prompt, modelId, false, temperature, topP, maxToken, createTime, webFunctionList,
        childAgentIds, agentType ?? 0, operationMode ?? 0, libraryIds, isCloud, autoAgentFlag ?? false, ttsModelId, asrModelId);
  }

  void translateFromDTO(AgentDTO agent) {
    id = agent.id;
    name = agent.name ?? "";
    iconPath = agent.icon ?? "";
    description = agent.description ?? "";
    modelId = agent.llmModelId ?? "";
    prompt = agent.prompt ?? "";
    temperature = agent.temperature ?? 0.0;
    maxToken = agent.maxTokens ?? 4096;
    topP = agent.topP ?? 0.0;
    isCloud = agent.isCloud;
    createTime = 0;
    functionList = [];
    if (agent.toolFunctionList != null) {
      for (var dto in agent.toolFunctionList!) {
        AgentToolFunction function = AgentToolFunction();
        function.translateFromDTO(dto);
        functionList?.add(function);
      }
    }
    libraryIds = agent.datasetIds;
    operationMode = agent.mode;
    agentType = agent.type;
    childAgentIds = agent.subAgentIds;
    toolOperationMode = 0;
    autoAgentFlag = agent.autoAgentFlag;
  }
}

@HiveType(typeId: HiveTypeIds.agentToolFunctionTypeId)
class AgentToolFunction {
  @HiveField(0)
  var toolId = "";
  @HiveField(1)
  var toolName = "";
  @HiveField(2)
  var functionName = "";
  @HiveField(3)
  var functionDescription = "";
  @HiveField(4)
  @Deprecated("deprecated in 0.2.0,not support")
  int operationMode = 0;
  @HiveField(5)
  String? requestMethod = "";
  @HiveField(6)
  bool? isThirdTool = false;

  FunctionDto translateToDTO() {
    return FunctionDto(toolId, functionName, requestMethod ?? "", 0, "");
  }

  void translateFromDTO(FunctionDto dto) {
    toolId = dto.toolId ?? "";
    toolName = "";
    functionName = dto.functionName;
    functionDescription = "";
    operationMode = 0;
    requestMethod = dto.requestMethod;
    isThirdTool = dto.protocol == "external";
  }
}
