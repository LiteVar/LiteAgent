import 'dart:core';

import 'package:get/get.dart';
import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/function.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';

import '../config/constants.dart';
import '../widgets/dialog/dialog_tool_edit.dart';

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
  @HiveField(4)
  List<String>? childAgentMessageList; //deprecated in 0.2.0
  @HiveField(5)
  String? toolName; //deprecated in 0.2.0
  @HiveField(6)
  List<Thought>? thoughtList;
  @HiveField(7)
  String? taskId = "";

  bool isLoading = false;
  bool isThoughtExpanded = true;
}

class ChatRole {
  static const int User = 0;
  static const int Agent = 1;
}

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

class ThoughtRoleType {
  static const String Tool = "tool";
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
  String? thirdSchemaText = "";

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
    }
    if (apiType == "basic" || apiType == "Basic") {
      apiKeyType = "Basic";
    } else if (apiType == "bearer" || apiType == "Bearer") {
      apiKeyType = "Bearer";
    }
    return ToolDTO(id, "", "", name, description, schemaType, schemaText, thirdSchemaText, apiText, apiKeyType, false, "", "");
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
  List<String>? toolList; //deprecated in 0.2.0
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

  AgentBean(
      {this.id = "",
      this.name = "",
      this.iconPath = "",
      this.description = "",
      this.modelId = "",
      this.toolList,
      this.prompt = "",
      this.temperature = 0.0,
      this.maxToken = 4096,
      this.topP = 1.0});

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

    return AgentDTO(id, "", "", name, iconPath, description, prompt, modelId, toolList, 0, false, temperature, topP, maxToken, createTime,
        "", webFunctionList, childAgentIds, agentType ?? 0, operationMode ?? 0, libraryIds, isCloud);
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
    this.isCloud = agent.isCloud;
    this.createTime = 0;
    this.functionList = [];
    if (agent.toolFunctionList != null) {
      for (var dto in agent.toolFunctionList!) {
        AgentToolFunction function = AgentToolFunction();
        function.translateFromDTO(dto);
        this.functionList?.add(function);
      }
    }
    this.libraryIds = agent.datasetIds;
    this.operationMode = agent.mode;
    this.agentType = agent.type;
    this.childAgentIds = agent.subAgentIds;
    this.toolOperationMode = 0;
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
  int operationMode = 0; //deprecated in 0.2.0
  @HiveField(5)
  String? requestMethod = "";
  @HiveField(6)
  bool? isThirdTool = false;

  FunctionDto translateToDTO() {
    return FunctionDto(toolId, functionName, requestMethod ?? "", 0, "");
  }

  void translateFromDTO(FunctionDto dto) {
    this.toolId = dto.toolId;
    this.toolName = "";
    this.functionName = dto.functionName;
    this.functionDescription = "";
    this.operationMode = 0;
    this.requestMethod = dto.requestMethod;
    this.isThirdTool = dto.protocol == "external";
  }
}
