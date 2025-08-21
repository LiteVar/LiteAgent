import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart' as server;
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/agent_detail.dart';
import 'package:lite_agent_client/models/dto/function.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:lite_agent_core_dart_server/lite_agent_core_dart_server.dart';
import 'package:opentool_dart/opentool_dart.dart' as opentool;
import 'package:yaml/yaml.dart';
import 'package:lite_agent_client/server/local_server/parser.dart';

import '../../repositories/account_repository.dart';
import '../../repositories/model_repository.dart';
import '../../repositories/tool_repository.dart';
import '../../utils/log_util.dart' as client_log;
import '../../widgets/dialog/dialog_tool_edit.dart';
import 'listener.dart';
import 'mcp_service.dart';

String baseUrl = "http://127.0.0.1:${config.server.port}${config.server.apiPathPrefix}";
Dio dio = Dio(BaseOptions(baseUrl: baseUrl));

class AgentLocalServer {
  String? agentId;
  SessionDto? _session;
  HttpClient? _httpClient;
  final Map<String, StreamSubscription<String>> _streamSubscriptions = {};
  Map<String, String> subAgentNameMap = {};
  MessageHandler? _messageHandler;

  Future<void> initChat(CapabilityDto capabilityDto) async {
    try {
      _session = await initSession(capabilityDto);
      _httpClient = HttpClient();
      if (_session != null) {
        client_log.Log.i("[initChat->RES] " + _session!.toJson().toString());
      }
    } catch (e) {
      client_log.Log.e("初始化聊天失败: $e");
      if (e is DioException) {
        client_log.Log.e("请求URL: ${e.requestOptions.uri}");
        client_log.Log.e("状态码: ${e.response?.statusCode}");
        client_log.Log.e("响应数据: ${e.response?.data}");
      }
    }
  }

  Future<SessionDto?> initSession(CapabilityDto capabilityDto) async {
    try {
      var response = await dio.post('/initSession', data: capabilityDto.toJson());
      SessionDto session = SessionDto.fromJson(response.data);
      return session;
    } catch (e) {
      client_log.Log.e("初始化聊天失败: $e");
      if (e is DioException) {
        client_log.Log.e("请求URL: ${e.requestOptions.uri}");
        client_log.Log.e("状态码: ${e.response?.statusCode}");
        client_log.Log.e("响应数据: ${e.response?.data}");
      }
    }
    return null;
  }

  Future<void> sendUserMessage(String prompt) async {
    String sessionId = _session?.sessionId ?? "";
    if (sessionId.isEmpty) {
      client_log.Log.e("Error: Session not created");
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
    UserTaskDto userTaskDto = UserTaskDto(content: [contentDto]);

    request.add(utf8.encode(jsonEncode(userTaskDto.toJson())));

    HttpClientResponse response = await request.close();
    Stream<String> stream = response.transform(utf8.decoder);

    // 监听SSE事件
    _streamSubscriptions[messageId] = stream.listen((data) {
      _onData(sessionId, messageId, data);
    }, onDone: () {
      client_log.Log.i('SSE connection closed for message $messageId');
      _streamSubscriptions.remove(messageId);
      _messageHandler?.removeMessage(messageId);
    }, onError: (error) {
      client_log.Log.e('SSE error for message $messageId: $error');
      _streamSubscriptions.remove(messageId);
    });
  }

  void _onData(String sessionId, String messageId, String data) {
    // 解析SSE格式数据
    final eventRegex = RegExp(r'event:(\w+)\ndata:(.*?)\n\n');
    final matches = eventRegex.allMatches(data);

    for (var match in matches) {
      final eventName = match.group(1);
      final eventData = match.group(2);

      if (eventName == SSEEventType.MESSAGE) {
        AgentMessageDto agentMessageDto = AgentMessageDto.fromJson(jsonDecode(eventData!));
        listen(sessionId, agentMessageDto);
        _messageHandler?.handle(sessionId, messageId, agentMessageDto);
      } else if (eventName == SSEEventType.CHUNK) {
        AgentMessageChunkDto agentMessageChunkDto = AgentMessageChunkDto.fromJson(jsonDecode(eventData!));
        listenChunk(sessionId, agentMessageChunkDto);
      } else if (eventName == SSEEventType.FUNCTION_CALL) {
        client_log.Log.d("Received client functionCall: ${eventData}");
      }
    }
  }

  setMessageHandler(MessageHandler messageHandler) {
    _messageHandler = messageHandler;
  }

  Future<void> stopChat() async {
    String sessionId = _session?.sessionId ?? "";
    if (sessionId.isEmpty) {
      return;
    }
    try {
      await dio.get('/stop', queryParameters: {"sessionId": sessionId});
    } catch (e) {
      client_log.Log.e(e);
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
    _messageHandler = null;
    subAgentNameMap.clear();

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
      int maxToken = min(model.maxTokens ?? 4096, agent.maxTokens ?? 4096);
      LLMConfigDto llmConfig = LLMConfigDto(
        baseUrl: model.baseUrl,
        apiKey: model.apiKey,
        model: model.name,
        temperature: agent.temperature ?? 0,
        maxTokens: maxToken,
        topP: agent.topP ?? 1,
        supportsToolCalling: model.toolInvoke ?? true,
        supportsDeepThinking: model.deepThink ?? false,
      );

      List<SessionNameDto> sessionList = [];

      String prompt = agent.prompt ?? "";

      var autoAgentFlag = agent.autoAgentFlag ?? false;
      var autoLLMConfigList = <LLMConfigDto>[];
      if (autoAgentFlag) {
        var autoModelList = await modelRepository.getCloudAutoAgentModelList();
        for (var model in autoModelList) {
          LLMConfigDto llmConfig = LLMConfigDto(
            baseUrl: model.baseUrl,
            apiKey: model.apiKey,
            model: model.name,
            supportsToolCalling: model.toolInvoke ?? true,
            supportsDeepThinking: model.deepThink ?? false,
          );
          autoLLMConfigList.add(llmConfig);
        }
      }

      //tool
      List<OpenSpecDto> openSpecDtoList = [];
      //openToolCallBack
      ClientOpenToolDto? clientOpenTool;

      if (autoAgentFlag) {
        var autoToolList = await toolRepository.getCloudAutoAgentToolList();
        var functionList = <FunctionDto>[];
        var openToolFunctionList = <FunctionDto>[];
        Map<String, ToolDTO> toolMap = {};
        for (var tool in autoToolList) {
          if (tool.functionList == null || tool.functionList!.isEmpty) {
            continue;
          }
          var toolDetail = await toolRepository.getCloudToolDetail(tool.id);
          if (toolDetail == null) {
            continue;
          }
          if (toolDetail.schemaType == 5) {
            continue;
          }
          toolMap[tool.id] = toolDetail;
          if (toolDetail.schemaType == 4) {
            for (var function in tool.functionList!) {
              function.toolId = tool.id;
              openToolFunctionList.add(function);
            }
          } else {
            for (var function in tool.functionList!) {
              function.toolId = tool.id;
              functionList.add(function);
            }
          }
        }
        //tool
        var list = await buildCloudOpenSpecDtoList(functionList, toolMap: toolMap);
        openSpecDtoList.addAll(list);

        //openToolCallBack
        if (openToolFunctionList.isNotEmpty) {
          var tool = toolMap[openToolFunctionList.first.toolId];
          List<String> functionNameList = openToolFunctionList.map((function) => function.functionName).toList();

          String openToolString = tool?.schemaStr ?? "";

          if (openToolString.isNotEmpty) {
            Map<String, dynamic> schemaMap = jsonDecode(openToolString);
            List functionArray = schemaMap["functions"];
            functionArray.removeWhere((json) => !functionNameList.contains(json["name"]));

            String newSchemaText = jsonEncode(schemaMap);
            clientOpenTool = ClientOpenToolDto(opentool: newSchemaText);
          }
        }
      } else {
        //tool
        var functionList = agentDetail.functionList?.where((item) => item.protocol != "external").toList();
        if (functionList != null) {
          var list = await buildCloudOpenSpecDtoList(functionList);
          openSpecDtoList.addAll(list);
        }

        //openToolCallBack
        var openToolFunctionList = agentDetail.functionList?.where((item) => item.protocol == "external").toList();
        if (openToolFunctionList != null && openToolFunctionList.isNotEmpty) {
          var tool = await toolRepository.getCloudToolDetail(openToolFunctionList.first.toolId ?? "");
          List<String> functionNameList = openToolFunctionList.map((function) => function.functionName).toList();

          String openToolString = tool?.schemaStr ?? "";

          if (openToolString.isNotEmpty) {
            Map<String, dynamic> schemaMap = jsonDecode(openToolString);
            List functionArray = schemaMap["functions"];
            functionArray.removeWhere((json) => !functionNameList.contains(json["name"]));

            String newSchemaText = jsonEncode(schemaMap);
            clientOpenTool = ClientOpenToolDto(opentool: newSchemaText);
          }
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
      var subAgentIds = agent.subAgentIds;
      List<ReflectPromptDto>? reflectPromptList;
      if (subAgentIds != null) {
        for (var agentId in subAgentIds) {
          var subAgent = await agentRepository.getCloudAgentDetail(agentId);
          var model = subAgent?.model;
          if (subAgent != null && model != null) {
            if (subAgent.agent?.type == AgentType.REFLECTION) {
              ReflectPromptDto reflectPromptDto = ReflectPromptDto(
                  llmConfig: LLMConfigDto(baseUrl: model.baseUrl, apiKey: model.apiKey, model: model.name),
                  prompt: subAgent.agent?.prompt ?? "");
              reflectPromptList ??= [];
              reflectPromptList.add(reflectPromptDto);

              continue;
            }

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

      bool? enableThinking;
      var isModelSupportDeepThinking = model.name.startsWith("qwen3") || model.name.startsWith("deepseek");
      if (isModelSupportDeepThinking) {
        enableThinking = model.deepThink ?? false;
      }

      return CapabilityDto(
        llmConfig: llmConfig,
        systemPrompt: prompt,
        openSpecList: openSpecDtoList,
        clientOpenTool: clientOpenTool,
        sessionList: sessionList,
        reflectPromptList: reflectPromptList,
        taskPipelineStrategy: taskType,
        toolPipelineStrategy: null,
        auto: autoAgentFlag,
        llmConfigList: autoLLMConfigList,
        enableThinking: enableThinking,
      );
    }

    return null;
  }

  Future<CapabilityDto?> buildLocalAgentCapabilityDto(AgentBean agent, {List<ModelBean>? autoModelList}) async {
    var model = await modelRepository.getModelFromBox(agent.modelId);
    if (model != null) {
      int maxToken = min(int.tryParse(model.maxToken ?? "4096") ?? 4096, agent.maxToken);
      try {
        LLMConfigDto llmConfig = LLMConfigDto(
          baseUrl: model.url,
          apiKey: model.key,
          model: model.name,
          temperature: agent.temperature,
          maxTokens: maxToken,
          topP: agent.topP,
          supportsToolCalling: model.supportToolCalling ?? true,
          supportsDeepThinking: model.supportDeepThinking ?? false,
        );

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

          String openToolString = tool?.schemaText ?? "";

          if (openToolString.isNotEmpty) {
            Map<String, dynamic> schemaMap = jsonDecode(openToolString);
            List functionArray = schemaMap["functions"];
            functionArray.removeWhere((json) => !functionNameList.contains(json["name"]));

            String newSchemaText = jsonEncode(schemaMap);
            clientOpenTool = ClientOpenToolDto(opentool: newSchemaText);
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
        var subAgents = agent.childAgentIds?.toList();
        List<ReflectPromptDto>? reflectPromptList;
        if (subAgents != null) {
          for (var agentId in subAgents) {
            var agent = await agentRepository.getAgentFromBox(agentId);
            if (agent != null) {
              if (agent.agentType == AgentType.REFLECTION) {
                var model = await modelRepository.getModelFromBox(agent.modelId);
                if (model != null) {
                  ReflectPromptDto reflectPromptDto = ReflectPromptDto(
                      llmConfig: LLMConfigDto(baseUrl: model.url, apiKey: model.key, model: model.name), prompt: agent.prompt);
                  reflectPromptList ??= [];
                  reflectPromptList.add(reflectPromptDto);
                }
                continue;
              }
              var capabilityDto = await buildLocalAgentCapabilityDto(agent); //Recursion
              if (capabilityDto != null) {
                var session = await initSession(capabilityDto);
                if (session != null) {
                  final description = agent.description.isEmpty ? null : agent.description;
                  sessionList.add(SessionNameDto(sessionId: session.sessionId, description: description));
                  subAgentNameMap[session.sessionId] = agent.name;
                }
              }
            }
          }
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

        var modelList = autoModelList ?? [];
        var autoAgentFlag = agent.autoAgentFlag ?? false;
        var autoLLMConfigList = <LLMConfigDto>[];
        if (autoAgentFlag && modelList.isNotEmpty) {
          for (var model in modelList) {
            LLMConfigDto llmConfig = LLMConfigDto(
              baseUrl: model.url,
              apiKey: model.key,
              model: model.name,
              supportsToolCalling: model.supportToolCalling ?? true,
              supportsDeepThinking: model.supportDeepThinking ?? false,
            );
            autoLLMConfigList.add(llmConfig);
          }
        }

        bool? enableThinking;
        var isModelSupportDeepThinking = model.name.startsWith("qwen3") || model.name.startsWith("deepseek");
        if (isModelSupportDeepThinking) {
          enableThinking = model.supportDeepThinking ?? false;
        }

        return CapabilityDto(
          llmConfig: llmConfig,
          systemPrompt: prompt,
          openSpecList: openSpecDtoList,
          sessionList: sessionList,
          clientOpenTool: clientOpenTool,
          reflectPromptList: reflectPromptList,
          taskPipelineStrategy: taskType,
          toolPipelineStrategy: toolPipelineStrategy,
          auto: autoAgentFlag,
          llmConfigList: autoLLMConfigList,
          enableThinking: enableThinking,
        );
      } catch (e) {
        client_log.Log.e(e);
      }
    }
    return null;
  }

  Future<OpenSpecDto> buildLibraryTool(List<String> libraryIds) async {
    String jsonString = await server.rootBundle.loadString('assets/json/library_tool.json');
    Map<String, dynamic> toolMap = jsonDecode(jsonString);
    String serverUrl = await accountRepository.getApiServerUrl();
    List<Map<String, String>> serversArray = [];
    for (var libraryId in libraryIds) {
      serversArray.add({"url": "$serverUrl/v1/dataset/$libraryId"});
    }
    toolMap["servers"] = serversArray;

    ApiKeyDto apiKey = ApiKeyDto(type: opentool.ApiKeyType.bearer, apiKey: await accountRepository.getOriginalToken(true));

    return OpenSpecDto(openSpec: jsonEncode(toolMap), protocol: Protocol.OPENAPI, apiKey: apiKey);
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
      if (tool != null) {
        String newSchemeText = "";
        String protocol = "";

        if (tool.schemaType == Protocol.MCP_STDIO_TOOLS) {
          final mcpService = McpService();
          final results = await mcpService.checkServers(tool.schemaText);
          final invalidServerIds = results.where((item) => !item.isAvailable).map((item) => item.serverId).toList();

          Map<String, dynamic> schemeMap = jsonDecode(tool.schemaText);
          schemeMap["mcpServers"].forEach((key, value) {
            if (!invalidServerIds.contains(key)) {
              OpenSpecDto openSpec = OpenSpecDto(openSpec: jsonEncode({key: value}), protocol: Protocol.MCP_STDIO_TOOLS);
              openSpecList.add(openSpec);
            }
          });
          continue;
        }

        var scheme = tool.schemaText;
        if (tool.schemaType == SchemaType.OPENAPI || tool.schemaType == Protocol.OPENAPI) {
          protocol = Protocol.OPENAPI;
          if (await scheme.isOpenAIJson()) {
          } else if (await scheme.isOpenAIYaml()) {
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
        } else if (tool.schemaType == SchemaType.JSONRPCHTTP || tool.schemaType == Protocol.JSONRPCHTTP) {
          protocol = Protocol.JSONRPCHTTP;
          if (await scheme.isOpenAIJson()) {
          } else if (await scheme.isOpenAIYaml()) {
            YamlMap yamlMap = loadYaml(scheme);
            scheme = jsonEncode(yamlMap);
          }

          Map<String, dynamic> schemeMap = jsonDecode(scheme);
          schemeMap["methods"].retainWhere((method) => functionNameList.contains(method["name"]));

          newSchemeText = jsonEncode(schemeMap);
        } else if (tool.schemaType == SchemaType.OPENMODBUS || tool.schemaType == Protocol.OPENMODBUS) {
          protocol = Protocol.OPENMODBUS;
          if (await scheme.isOpenAIJson()) {
          } else if (await scheme.isOpenAIYaml()) {
            YamlMap yamlMap = loadYaml(scheme);
            scheme = jsonEncode(yamlMap);
          }

          Map<String, dynamic> schemeMap = jsonDecode(scheme);
          schemeMap["functions"].retainWhere((method) => functionNameList.contains(method["name"]));

          newSchemeText = jsonEncode(schemeMap);
        }
        if (protocol.isEmpty || newSchemeText.isEmpty) {
          continue;
        }

        ApiKeyDto? apiKey;
        if (tool.apiText.isNotEmpty) {
          if (tool.apiType == "basic" || tool.apiType == "Basic") {
            apiKey = ApiKeyDto(type: opentool.ApiKeyType.basic, apiKey: tool.apiText);
          } else if (tool.apiType == "bearer" || tool.apiType == "Bearer") {
            apiKey = ApiKeyDto(type: opentool.ApiKeyType.bearer, apiKey: tool.apiText);
          } else {
            apiKey = ApiKeyDto(type: opentool.ApiKeyType.original, apiKey: tool.apiText);
          }
        }
        OpenSpecDto openSpec = OpenSpecDto(openSpec: newSchemeText, protocol: protocol, apiKey: apiKey);
        openSpecList.add(openSpec);
      }
    }
    //openSpecList.forEach((item) => print("openSpec${openSpecList.indexOf(item)}: ${item.protocol}"));
    return openSpecList;
  }

  Future<List<OpenSpecDto>> buildCloudOpenSpecDtoList(List<FunctionDto> functionList, {Map<String, ToolDTO>? toolMap}) async {
    List<OpenSpecDto> openSpecList = [];
    var toolFunctionMap = <String, List<String>>{};
    var functionMethodMap = <String, List<String>>{};

    for (var function in functionList) {
      var toolId = function.toolId;
      if (toolId == null || toolId.isEmpty) {
        continue;
      }
      if (!toolFunctionMap.containsKey(toolId)) {
        toolFunctionMap[toolId] = [];
      }
      toolFunctionMap[toolId]?.add(function.functionName);

      String key = "$toolId${function.functionName}";
      if (!functionMethodMap.containsKey(key)) {
        functionMethodMap[key] = [];
      }
      functionMethodMap[key]?.add(function.requestMethod ?? "");
    }

    for (var entry in toolFunctionMap.entries) {
      var toolId = entry.key;
      var functionNameList = entry.value;
      ToolDTO? tool;
      if (toolMap != null && toolMap.containsKey(toolId)) {
        tool = toolMap[toolId];
      } else {
        tool = await toolRepository.getCloudToolDetail(toolId);
      }
      String newSchemeText = "";
      String protocol = "";
      if (tool != null) {
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
          case 4: //open_tool
          case 5: //mcp(sse)
            continue;
        }

        var scheme = tool.schemaStr ?? "";
        if (protocol == Protocol.OPENAPI) {
          if (await scheme.isOpenAIJson()) {
          } else if (await scheme.isOpenAIYaml()) {
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
        } else if (protocol == Protocol.JSONRPCHTTP) {
          if (await scheme.isOpenAIJson()) {
          } else if (await scheme.isOpenAIYaml()) {
            YamlMap yamlMap = loadYaml(scheme);
            scheme = jsonEncode(yamlMap);
          }

          Map<String, dynamic> schemeMap = jsonDecode(scheme);
          schemeMap["methods"].retainWhere((method) => functionNameList.contains(method["name"]));

          newSchemeText = jsonEncode(schemeMap);
        } else if (protocol == Protocol.OPENMODBUS) {
          if (await scheme.isOpenAIJson()) {
          } else if (await scheme.isOpenAIYaml()) {
            YamlMap yamlMap = loadYaml(scheme);
            scheme = jsonEncode(yamlMap);
          }

          Map<String, dynamic> schemeMap = jsonDecode(scheme);
          schemeMap["functions"].retainWhere((method) => functionNameList.contains(method["name"]));

          newSchemeText = jsonEncode(schemeMap);
        }

        if (protocol.isEmpty || newSchemeText.isEmpty) {
          continue;
        }

        ApiKeyDto? apiKey;
        String apiKeyText = tool.apiKey ?? "";
        if (apiKeyText.isNotEmpty) {
          if (tool.apiKeyType == "basic" || tool.apiKeyType == "Basic") {
            apiKey = ApiKeyDto(type: opentool.ApiKeyType.basic, apiKey: apiKeyText);
          } else if (tool.apiKeyType == "bearer" || tool.apiKeyType == "Bearer") {
            apiKey = ApiKeyDto(type: opentool.ApiKeyType.bearer, apiKey: apiKeyText);
          } else {
            apiKey = ApiKeyDto(type: opentool.ApiKeyType.original, apiKey: apiKeyText);
          }
        }

        OpenSpecDto openSpec = OpenSpecDto(openSpec: newSchemeText, protocol: protocol, apiKey: apiKey);
        openSpecList.add(openSpec);
      }
    }

    return openSpecList;
  }
}
