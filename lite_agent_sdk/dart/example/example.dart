import 'package:liteagent_sdk_dart/liteagent_sdk_dart.dart';
import 'package:opentool_dart/opentool_dart.dart';

Future<void> main() async {
  String baseUrl = "<BASE_URL>";
  String apiKey = "<API_KEY>";
  String userPrompt = "hi";
  String agentId = "<AGENT_ID>";
  LiteAgentSDK liteAgent = LiteAgentSDK(baseUrl: baseUrl, apiKey: apiKey);
  Session session = await liteAgent.initSession(agentId);
  UserTask userTaskDto = UserTask(content: [Content(type: ContentType.text, message: userPrompt)], stream: true);
  AgentMessageHandler agentMessageHandler = AgentMessageHandlerImpl();
  await liteAgent.chat(session, userTaskDto, agentMessageHandler);
}

class AgentMessageHandlerImpl extends AgentMessageHandler {
  @override
  Future<ToolReturn> onFunctionCall(String sessionId, FunctionCall functionCall) async {
    print(functionCall.toJson().toString());
    return ToolReturn(id: functionCall.id, result: {"name": functionCall.name, "params": {"status": "success"}});
  }

  @override
  Future<void> onDone() async {
    print("[onDone]");
  }

  @override
  Future<void> onError(Exception e) async {
    print("[onError]$e");
  }

  @override
  Future<void> onMessage(String sessionId, AgentMessage agentMessageDto) async {
    print("sessionId: $sessionId, agentMessage: ${agentMessageDto.toJson().toString()}");
  }

  @override
  Future<void> onChunk(String sessionId, AgentMessageChunk agentMessageChunkDto) async {
    print("sessionId: $sessionId, agentMessageChunk: agentMessageChunkDto.toJson().toString()}");
  }
}