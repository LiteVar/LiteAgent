import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:lite_agent_client/models/dto/agent_detail.dart';
import 'package:lite_agent_client/models/dto/function.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:lite_agent_core_dart_server/lite_agent_core_dart_server.dart';
import 'package:opentool_dart/opentool_dart.dart' as opentool;
import 'package:yaml/yaml.dart';

import '../../repositories/account_repository.dart';
import '../../repositories/model_repository.dart';
import '../../repositories/tool_repository.dart';
import '../../widgets/dialog/dialog_tool_edit.dart';

String baseUrl = "http://127.0.0.1:${config.server.port}${config.server.apiPathPrefix}";
Dio dio = Dio(BaseOptions(baseUrl: baseUrl));

class AgentLocalServer {
  String? agentId;
  SessionDto? _session;
  HttpClient? _httpClient;
  Map<String, StreamSubscription<String>> _streamSubscriptions = {};
  Function(AgentMessageDto)? onUserReceive;

  Future<void> initChat(CapabilityDto capabilityDto, Function(AgentMessageDto) onUserReceive) async {
    try {
      _session = await initSession(capabilityDto);
      _httpClient = HttpClient();
      if (_session != null) {
        print("[initChat->RES] " + _session!.toJson().toString());
      }
      this.onUserReceive = onUserReceive;
    } catch (e) {
      print("初始化聊天失败: $e");
      if (e is DioException) {
        print("请求URL: ${e.requestOptions.uri}");
        print("状态码: ${e.response?.statusCode}");
        print("响应数据: ${e.response?.data}");
      }
    }
  }

  Future<SessionDto?> initSession(CapabilityDto capabilityDto) async {
    try {
      var response = await dio.post('/initSession', data: capabilityDto.toJson());
      SessionDto session = SessionDto.fromJson(response.data);
      return session;
    } catch (e) {
      print("初始化聊天失败: $e");
      if (e is DioException) {
        print("请求URL: ${e.requestOptions.uri}");
        print("状态码: ${e.response?.statusCode}");
        print("响应数据: ${e.response?.data}");
      }
    }
    return null;
  }

  Future<void> sendUserMessage(String prompt) async {
    String sessionId = _session?.sessionId ?? "";
    if (sessionId.isEmpty) {
      print("Error: Session not created");
      return;
    }

    _httpClient ??= HttpClient();

    String messageId = DateTime.now().millisecondsSinceEpoch.toString();

    // 创建SSE连接请求
    Uri uri = Uri.parse('$baseUrl/chat?sessionId=$sessionId');
    HttpClientRequest request = await _httpClient!.postUrl(uri);
    request.headers.add(HttpHeaders.acceptHeader, 'text/event-stream');
    request.headers.add(HttpHeaders.contentTypeHeader, 'application/json');

    ContentDto contentDto = ContentDto(type: ContentType.TEXT, message: prompt);
    UserTaskDto userTaskDto = UserTaskDto(contentList: [contentDto]);

    request.add(utf8.encode(jsonEncode(userTaskDto.toJson())));

    HttpClientResponse response = await request.close();
    Stream<String> stream = response.transform(utf8.decoder);

    // 监听SSE事件
    _streamSubscriptions[messageId] = stream.listen((data) {
      _onData(sessionId, messageId, data);
    }, onDone: () {
      print('SSE connection closed for message $messageId');
      _streamSubscriptions.remove(messageId);
    }, onError: (error) {
      print('SSE error for message $messageId: $error');
      _streamSubscriptions.remove(messageId);
    });
  }

  void _onData(String sessionId, String messageId, String data) {
    // 解析SSE格式数据
    final eventRegex = RegExp(r'event: (\w+)\ndata: (.*?)\n\n');
    final matches = eventRegex.allMatches(data);

    for (var match in matches) {
      final eventName = match.group(1);
      final eventData = match.group(2);

      if (eventName == SSEEventType.MESSAGE) {
        AgentMessageDto agentMessageDto = AgentMessageDto.fromJson(jsonDecode(eventData!));
        if (onUserReceive != null) {
          onUserReceive!(agentMessageDto);
        }
      } else if (eventName == SSEEventType.CHUNK) {
        AgentMessageChunkDto agentMessageChunkDto = AgentMessageChunkDto.fromJson(jsonDecode(eventData!));
        // print("Received chunk: ${agentMessageChunkDto.part}");
      } else if (eventName == SSEEventType.FUNCTION_CALL) {
        print("Received client functionCall: ${eventData}");
      }
    }
    return null;
  }

  Future<void> stopChat() async {
    String sessionId = _session?.sessionId ?? "";
    if (sessionId.isEmpty) {
      return;
    }
    try {
      await dio.get('/stop', queryParameters: {"sessionId": sessionId});
    } catch (e) {
      print(e);
    }
  }

  Future<void> clearChat() async {
    String sessionId = _session?.sessionId ?? "";
    if (sessionId.isEmpty) {
      return;
    }
    await dio.get('/clear', queryParameters: {"sessionId": sessionId});

    // 取消所有活跃的订阅
    for (var subscription in _streamSubscriptions.values) {
      await subscription.cancel();
    }
    _streamSubscriptions.clear();
    onUserReceive == null;

    _httpClient?.close();
    _httpClient = null;
  }

  bool isConnecting() {
    return _session != null && _httpClient != null;
  }

  Future<CapabilityDto?> buildCloudAgentCapabilityDto(AgentDetailDTO agentDetail) async {
    var model = agentDetail.model;
    var agent = agentDetail.agent;
    if (model != null && agent != null) {
      LLMConfigDto llmConfig = LLMConfigDto(
          baseUrl: model.baseUrl,
          apiKey: model.apiKey,
          model: model.name,
          temperature: agent.temperature ?? 0,
          maxTokens: agent.maxTokens ?? 4096,
          topP: agent.topP ?? 1);

      List<SessionNameDto> sessionList = [];

      String prompt = agent.prompt ?? "";

      //tool
      List<OpenSpecDto> openSpecDtoList = [];
      var functionList = agentDetail.functionList?.where((item) => item.protocol != "external").toList();
      if (functionList != null) {
        var list = await buildCloudOpenSpecDtoList(functionList);
        openSpecDtoList.addAll(list);
      }

      //openToolCallBack
      ClientOpenToolDto? clientOpenTool;
      var openToolFunctionList = agentDetail.functionList?.where((item) => item.protocol == "external").toList();
      if (openToolFunctionList != null && openToolFunctionList.isNotEmpty) {
        var tool = await toolRepository.getCloudToolDetail(openToolFunctionList.first.toolId);
        List<String> functionNameList = openToolFunctionList.map((function) => function.functionName).toList();

        String openToolString = tool?.openSchemaStr ?? "";

        if (openToolString.isNotEmpty) {
          Map<String, dynamic> schemaMap = jsonDecode(openToolString);
          List functionArray = schemaMap["functions"];
          functionArray.removeWhere((json) => !functionNameList.contains(json["name"]));

          String newSchemaText = jsonEncode(schemaMap);
          clientOpenTool = ClientOpenToolDto(opentool: newSchemaText);
          //print("newSchemaText:$newSchemaText");
        }
      }

      //library
      var libraryList = agentDetail.datasetList?.toList();
      if (libraryList != null && libraryList.isNotEmpty) {
        List<String> libraryIds = [];
        for (var library in libraryList) {
          libraryIds.add(library.id);
        }
        openSpecDtoList.add(await buildLibraryTool(libraryIds));
      }

      //childAgent
      var childAgentList = agent.subAgentIds;
      if (childAgentList != null) {
        for (var agentId in childAgentList) {
          var subAgent = await agentRepository.getCloudAgentDetail(agentId);
          if (subAgent != null) {
            var capabilityDto = await buildCloudAgentCapabilityDto(subAgent); //Recursion
            if (capabilityDto != null) {
              var session = await initSession(capabilityDto);
              if (session != null) {
                String? description = subAgent.agent?.description ?? "";
                description = description.isEmpty ? null : description;
                sessionList.add(SessionNameDto(sessionId: session.sessionId, description: description));
              }
            }
          }
        }
      }

      List<ReflectPromptDto>? reflectPromptList;
      if (agent.type == AgentType.REFLECTION) {
        reflectPromptList = [ReflectPromptDto(llmConfig: llmConfig, prompt: prompt)];
      }
      String taskType;
      if (agent.mode == OperationMode.SERIAL) {
        taskType = PipelineStrategyType.SERIAL;
      } else if (agent.mode == OperationMode.REJECT) {
        taskType = PipelineStrategyType.REJECT;
      } else {
        taskType = PipelineStrategyType.PARALLEL;
      }
      /*String toolPipelineStrategy;
      if (agent.toolOperationMode == OperationMode.SERIAL) {
        toolPipelineStrategy = PipelineStrategyType.SERIAL;
      } else if (agent.toolOperationMode == OperationMode.REJECT) {
        toolPipelineStrategy = PipelineStrategyType.REJECT;
      } else {
        toolPipelineStrategy = PipelineStrategyType.PARALLEL;
      }*/

      return CapabilityDto(
          llmConfig: llmConfig,
          systemPrompt: prompt,
          openSpecList: openSpecDtoList,
          clientOpenTool: clientOpenTool,
          sessionList: sessionList,
          // timeoutSeconds: 20,
          reflectPromptList: reflectPromptList,
          taskPipelineStrategy: taskType,
          toolPipelineStrategy: null);
    }

    return null;
  }

  Future<CapabilityDto?> buildLocalAgentCapabilityDto(AgentBean agent) async {
    var model = await modelRepository.getModelFromBox(agent.modelId);
    if (model != null) {
      try {
        LLMConfigDto llmConfig = LLMConfigDto(
            baseUrl: model.url,
            apiKey: model.key,
            model: model.name,
            temperature: agent.temperature,
            maxTokens: agent.maxToken,
            topP: agent.topP);

        List<SessionNameDto> sessionList = [];

        String prompt = agent.prompt;

        List<OpenSpecDto> openSpecDtoList = [];

        //tool
        var functionList = agent.functionList?.where((item) => !(item.isThirdTool ?? false)).toList();
        if (functionList != null) {
          var list = await buildOpenSpecDtoList(functionList);
          openSpecDtoList.addAll(list);
        }

        //openToolCallBack
        ClientOpenToolDto? clientOpenTool;
        var openToolFunctionList = agent.functionList?.where((item) => item.isThirdTool ?? false).toList();
        if (openToolFunctionList != null && openToolFunctionList.isNotEmpty) {
          var tool = await toolRepository.getToolFromBox(openToolFunctionList.first.toolId);
          List<String> functionNameList = openToolFunctionList.map((function) => function.functionName).toList();

          String openToolString = tool?.thirdSchemaText ?? "";

          if (openToolString.isNotEmpty) {
            Map<String, dynamic> schemaMap = jsonDecode(openToolString);
            List functionArray = schemaMap["functions"];
            functionArray.removeWhere((json) => !functionNameList.contains(json["name"]));

            String newSchemaText = jsonEncode(schemaMap);
            clientOpenTool = ClientOpenToolDto(opentool: newSchemaText);
            //print("newSchemaText:$newSchemaText");
          }
        }

        //library
        if (await accountRepository.isLogin()) {
          var libraryIds = agent.libraryIds?.toList();
          if (libraryIds != null && libraryIds.isNotEmpty) {
            openSpecDtoList.add(await buildLibraryTool(libraryIds));
          }
        }

        //childAgent
        var childAgentList = agent.childAgentIds?.toList();
        if (childAgentList != null) {
          for (var agentId in childAgentList) {
            var agent = await agentRepository.getAgentFromBox(agentId);
            if (agent != null) {
              var capabilityDto = await buildLocalAgentCapabilityDto(agent); //Recursion
              if (capabilityDto != null) {
                var session = await initSession(capabilityDto);
                if (session != null) {
                  final description = agent.description.isEmpty ? null : agent.description;
                  sessionList.add(SessionNameDto(sessionId: session.sessionId, description: description));
                }
              }
            }
          }
        }

        List<ReflectPromptDto>? reflectPromptList;
        if (agent.agentType == AgentType.REFLECTION) {
          reflectPromptList = [ReflectPromptDto(llmConfig: llmConfig, prompt: prompt)];
        }
        String taskType;
        if (agent.operationMode == OperationMode.SERIAL) {
          taskType = PipelineStrategyType.SERIAL;
        } else if (agent.operationMode == OperationMode.REJECT) {
          taskType = PipelineStrategyType.REJECT;
        } else {
          taskType = PipelineStrategyType.PARALLEL;
        }
        String toolPipelineStrategy;
        if (agent.toolOperationMode == OperationMode.SERIAL) {
          toolPipelineStrategy = PipelineStrategyType.SERIAL;
        } else if (agent.toolOperationMode == OperationMode.REJECT) {
          toolPipelineStrategy = PipelineStrategyType.REJECT;
        } else {
          toolPipelineStrategy = PipelineStrategyType.PARALLEL;
        }

        return CapabilityDto(
            llmConfig: llmConfig,
            systemPrompt: prompt,
            openSpecList: openSpecDtoList,
            sessionList: sessionList,
            clientOpenTool: clientOpenTool,
            // timeoutSeconds: 20,
            reflectPromptList: reflectPromptList,
            taskPipelineStrategy: taskType,
            toolPipelineStrategy: toolPipelineStrategy);
      } catch (e) {
        print(e);
      }
    }
    return null;
  }

  Future<OpenSpecDto> buildLibraryTool(List<String> libraryIds) async {
    String jsonString = await rootBundle.loadString('assets/json/library_tool.json');
    Map<String, dynamic> toolMap = jsonDecode(jsonString);
    String serverUrl = await accountRepository.getApiServerUrl();
    List<Map<String, String>> serversArray = [];
    for (var libraryId in libraryIds) {
      serversArray.add({"url": "$serverUrl/v1/dataset/$libraryId"});
    }
    toolMap["servers"] = serversArray;

    String headerJsonString = await rootBundle.loadString('assets/json/header_parameters.json');
    Map<String, dynamic> headerJson = jsonDecode(headerJsonString);
    headerJson["schema"]["default"] = await accountRepository.getApiToken();

    toolMap["paths"]["/retrieve"]["get"]["parameters"].add(headerJson);
    //print(jsonEncode(toolMap));
    return OpenSpecDto(openSpec: jsonEncode(toolMap), protocol: Protocol.OPENAPI, apiKey: null);
  }

  Future<List<OpenSpecDto>> buildOpenSpecDtoList(List<AgentToolFunction> functionList) async {
    List<OpenSpecDto> openSpecList = [];
    var toolFunctionMap = <String, List<String>>{};
    var functionMethodMap = <String, List<String>>{};

    for (var function in functionList) {
      //third party open tool
      if (function.isThirdTool ?? false) {
        continue;
      }
      if (!toolFunctionMap.containsKey(function.toolId)) {
        toolFunctionMap[function.toolId] = [];
      }
      toolFunctionMap[function.toolId]?.add(function.functionName);

      if (function.requestMethod != null) {
        String key = "${function.toolId}${function.functionName}";
        if (!functionMethodMap.containsKey(key)) {
          functionMethodMap[key] = [];
        }
        functionMethodMap[key]?.add(function.requestMethod ?? "");
      }
    }

    for (var entry in toolFunctionMap.entries) {
      var toolId = entry.key;
      var functionNameList = entry.value;
      //注意:不能直接修改当前变量属性
      var tool = await toolRepository.getToolFromBox(toolId);
      String newSchemeText = "";
      if (tool != null) {
        var scheme = tool.schemaText;
        if (scheme.isNotEmpty) {
          if (await scheme.isOpenAIJson()) {
          } else if (scheme.isOpenAIYaml()) {
            YamlMap yamlMap = loadYaml(scheme);
            scheme = jsonEncode(yamlMap);
          }

          Map<String, dynamic> schemeMap = jsonDecode(scheme);
          schemeMap["paths"].removeWhere((key, value) => !functionNameList.contains(key));
          schemeMap["paths"].forEach((name, method) {
            String key = "$toolId$name";
            List<String>? methodList = functionMethodMap[key];
            if (methodList != null) {
              method.removeWhere((key, value) => !methodList.contains(key));
            }
          });

          newSchemeText = jsonEncode(schemeMap);

          //print("newSchemeText: $newSchemeText");
        }

        if (newSchemeText.isEmpty) {
          continue;
        }
        String protocol = "";
        if (tool.schemaType == SchemaType.OPENAPI || tool.schemaType == Protocol.OPENAPI) {
          protocol = Protocol.OPENAPI;
        } else if (tool.schemaType == SchemaType.JSONRPCHTTP || tool.schemaType == Protocol.JSONRPCHTTP) {
          protocol = Protocol.JSONRPCHTTP;
        } else if (tool.schemaType == SchemaType.OPENMODBUS || tool.schemaType == Protocol.OPENMODBUS) {
          protocol = Protocol.OPENMODBUS;
        }
        if (protocol.isEmpty) {
          continue;
        }
        ApiKeyDto? apiKey;
        if (tool.apiType == "basic" || tool.apiType == "Basic") {
          apiKey = ApiKeyDto(type: opentool.ApiKeyType.basic, apiKey: tool.apiText);
        } else if (tool.apiType == "bearer" || tool.apiType == "Bearer") {
          apiKey = ApiKeyDto(type: opentool.ApiKeyType.bearer, apiKey: tool.apiText);
        }
        OpenSpecDto openSpec = OpenSpecDto(openSpec: newSchemeText, protocol: protocol, apiKey: apiKey);
        openSpecList.add(openSpec);
      }
    }

    return openSpecList;
  }

  Future<List<OpenSpecDto>> buildCloudOpenSpecDtoList(List<FunctionDto> functionList) async {
    List<OpenSpecDto> openSpecList = [];
    var toolFunctionMap = <String, List<String>>{};
    var functionMethodMap = <String, List<String>>{};

    for (var function in functionList) {
      if (!toolFunctionMap.containsKey(function.toolId)) {
        toolFunctionMap[function.toolId] = [];
      }
      toolFunctionMap[function.toolId]?.add(function.functionName);

      String key = "${function.toolId}${function.functionName}";
      if (!functionMethodMap.containsKey(key)) {
        functionMethodMap[key] = [];
      }
      functionMethodMap[key]?.add(function.requestMethod ?? "");
    }

    for (var entry in toolFunctionMap.entries) {
      var toolId = entry.key;
      var functionNameList = entry.value;
      var tool = await toolRepository.getCloudToolDetail(toolId);
      String newSchemeText = "";
      if (tool != null) {
        var scheme = tool.schemaStr;
        if (scheme != null && scheme.isNotEmpty) {
          if (await scheme.isOpenAIJson()) {
          } else if (scheme.isOpenAIYaml()) {
            String cleanedText = scheme.replaceAll(r'\n', '\n').trim();
            YamlMap yamlMap = loadYaml(cleanedText);
            scheme = jsonEncode(yamlMap);
          }

          Map<String, dynamic> schemeMap = jsonDecode(scheme);
          schemeMap["paths"].removeWhere((key, value) => !functionNameList.contains(key));
          schemeMap["paths"].forEach((name, method) {
            String key = "$toolId$name";
            List<String>? methodList = functionMethodMap[key];
            if (methodList != null) {
              method.removeWhere((key, value) => !methodList.contains(key));
            }
          });

          newSchemeText = jsonEncode(schemeMap);
          //print("newSchemeText: $newSchemeText");
        }

        if (newSchemeText.isEmpty) {
          continue;
        }
        String protocol = "";
        switch (tool.schemaType) {
          case 1: //openapi
            protocol = Protocol.OPENAPI;
            break;
          case 2: //jsonrpc
            protocol = Protocol.JSONRPCHTTP;
            break;
          case 3: //open_modbus
            protocol = Protocol.OPENMODBUS;
            break;
        }
        if (protocol.isEmpty) {
          continue;
        }
        ApiKeyDto? apiKey;
        if (tool.apiKeyType == "basic" || tool.apiKeyType == "Basic") {
          apiKey = ApiKeyDto(type: opentool.ApiKeyType.basic, apiKey: tool.apiKey ?? "");
        } else if (tool.apiKeyType == "bearer" || tool.apiKeyType == "Bearer") {
          apiKey = ApiKeyDto(type: opentool.ApiKeyType.bearer, apiKey: tool.apiKey ?? "");
        }
        OpenSpecDto openSpec = OpenSpecDto(openSpec: newSchemeText, protocol: protocol, apiKey: apiKey);
        openSpecList.add(openSpec);
      }
    }

    return openSpecList;
  }
}
