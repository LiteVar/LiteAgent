import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';

import 'config.dart';

Dio dio = Dio(BaseOptions(baseUrl: "http://127.0.0.1:${config.server.port}${config.server.apiPathPrefix}"));

class AgentLocalServer {
  String? agentId;
  SessionDto? _session;
  WebSocket? _socket;

  Future<void> initChat(CapabilityDto capabilityDto) async {
    try {
      Response response = await dio.post('/init', data: capabilityDto.toJson());
      final payload = response.data as String;
      final data = jsonDecode(payload);
      _session = SessionDto.fromJson(data);
      if (_session != null) {
        print("[initChat->RES] " + _session!.toJson().toString());
      }
    } catch (e) {
      print(e);
    }
  }

  Future<void> connectChat(Function(ChatMessage) onUserReceive) async {
    String sessionId = _session?.id ?? "";
    if (sessionId.isEmpty) {
      return;
    }
    final String url = 'ws://127.0.0.1:${config.server.port}${config.server.apiPathPrefix}/chat?id=$sessionId';

    _socket = await WebSocket.connect(url);

    _socket?.listen((message) {
      final payload = message as String;
      if (payload != "pong") {
        final data = jsonDecode(payload);
        AgentMessageDto agentMessageDto = AgentMessageDto.fromJson(data);
        printAgentMessage(agentMessageDto, onUserReceive);
      } else {
        print("[pong]");
      }
    }, onDone: () => print('WebSocket connection closed'), onError: (error) => print('WebSocket error: $error'), cancelOnError: true);
  }

  Future<void> sendUserMessage(String prompt) async {
    if (_socket == null) {
      return;
    }
    UserMessageDto userMessageDto = UserMessageDto(type: UserMessageDtoType.text, message: prompt);
    UserTaskDto userTaskDto = UserTaskDto(taskId: "0", contentList: [userMessageDto]);
    _socket?.add(jsonEncode(userTaskDto.toJson()));
  }

  Future<void> sendPing() async {
    _socket?.add("ping");
  }

  Future<void> stopChat() async {
    String sessionId = _session?.id ?? "";
    if (sessionId.isEmpty) {
      return;
    }
    try {
      await dio.get('/stop', queryParameters: {"id": sessionId});
    } catch (e) {
      print(e);
    }
  }

  Future<void> clearChat() async {
    String sessionId = _session?.id ?? "";
    if (sessionId.isEmpty || _socket == null) {
      return;
    }
    await dio.get('/clear', queryParameters: {"id": sessionId});
    await _socket?.close();
  }

  void printAgentMessage(AgentMessageDto agentMessageDto, Function(ChatMessage) onUserReceive) {
    String system = "🖥SYSTEM";
    String user = "👤USER";
    String agent = "🤖AGENT";
    String llm = "💡LLM";
    String tool = "🔧TOOL";
    String client = "🔗CLIENT";

    String message = "";
    if (agentMessageDto.type == AgentMessageType.text) message = agentMessageDto.message as String;
    if (agentMessageDto.type == AgentMessageType.imageUrl) message = agentMessageDto.message as String;
    if (agentMessageDto.type == AgentMessageType.functionCallList) {
      List<dynamic> originalFunctionCallList = agentMessageDto.message as List<dynamic>;
      List<FunctionCall> functionCallList = originalFunctionCallList.map((dynamic json) {
        return FunctionCall.fromJson(json);
      }).toList();
      message = jsonEncode(functionCallList);
    }
    if (agentMessageDto.type == AgentMessageType.toolReturn) {
      message = jsonEncode(ToolReturn.fromJson(agentMessageDto.message));
    }
    ;

    String from = "";
    if (agentMessageDto.from == ToolRoleType.SYSTEM) {
      from = system;
      message = "\n$message";
    }
    if (agentMessageDto.from == ToolRoleType.USER) from = user;
    if (agentMessageDto.from == ToolRoleType.AGENT) from = agent;
    if (agentMessageDto.from == ToolRoleType.LLM) from = llm;
    if (agentMessageDto.from == ToolRoleType.TOOL) from = tool;
    if (agentMessageDto.from == ToolRoleType.CLIENT) from = client;

    String to = "";
    if (agentMessageDto.to == ToolRoleType.SYSTEM) to = system;
    if (agentMessageDto.to == ToolRoleType.USER) to = user;
    if (agentMessageDto.to == ToolRoleType.AGENT) to = agent;
    if (agentMessageDto.to == ToolRoleType.LLM) to = llm;
    if (agentMessageDto.to == ToolRoleType.TOOL) to = tool;
    if (agentMessageDto.to == ToolRoleType.CLIENT) to = client;

    if (from.isNotEmpty && to.isNotEmpty) {
      print("#${agentMessageDto.sessionId}# $from -> $to: [${agentMessageDto.type}] $message");
    }
    if (agentMessageDto.to == ToolRoleType.USER) {
      ChatMessage chatMessage = ChatMessage();
      chatMessage.sendRole = ChatRole.Agent;
      chatMessage.message = message;
      onUserReceive(chatMessage);
    }
  }

  bool isConnecting() {
    return _session != null && _socket != null;
  }
}

class Server {
  late String ip;
  late String apiPathPrefix;
  late int port;

  Server({required this.ip, required this.apiPathPrefix, required this.port});
}

class ToolRoleType {
  static const String SYSTEM = "system"; // system prompt
  static const String USER = "user"; // user
  static const String AGENT = "agent"; // agent
  static const String LLM = "llm"; // llm
  static const String TOOL = "tool"; // external tools
  static const String CLIENT = "client"; // external caller
}
