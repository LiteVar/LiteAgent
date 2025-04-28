import 'dart:convert';
import 'package:dotenv/dotenv.dart';
import 'package:liteagent_sdk_dart/liteagent_sdk_dart.dart';
import 'package:opentool_dart/opentool_dart.dart';

String baseUrl = "http://127.0.0.1:9527/api/v1";

String userPrompt = "hi";

Future<void> main() async {
  LiteAgentSDK liteAgent = LiteAgentSDK(baseUrl: baseUrl);
  Version version = await liteAgent.getVersion();

  Capability capabilityDto = Capability(
    llmConfig: _buildLLMConfigDto(),
    systemPrompt: _buildSystemPrompt(),
    timeoutSeconds: 20
  );

  Agent agentDto = Agent(name: "Storage Manager", capability: capabilityDto);

  AgentId agentIdDto = await liteAgent.createAgent(agentDto);

  List<AgentInfo> agentInfoDtoList = await liteAgent.listAgent();

  agentInfoDtoList.forEach((agentInfoDto) => print(agentInfoDto.toJson()));

  Session session = await liteAgent.initSession(agentIdDto.agentId);

  UserTask userTaskDto = UserTask(content: [Content(type: ContentType.text, message: userPrompt)], stream: true);

  print(jsonEncode(userTaskDto.toJson()));

  AgentMessageHandler agentMessageHandler1 = AgentMessageHandlerImpl();
  await liteAgent.chat(session, userTaskDto, agentMessageHandler1);

  AgentMessageHandler agentMessageHandler2 = AgentMessageHandlerImpl();
  await liteAgent.chat(session, userTaskDto, agentMessageHandler2);

  await sleep(30);

  List<AgentMessage> agentMessageDtoList = await liteAgent.getHistory(session);

  agentMessageDtoList.forEach((agentMessageDto) => print(agentMessageDto.toJson()));

  await sleep(5);

}

Future<void> sleep(int seconds) async {
  for (int i = seconds; i > 0; i--) {
    print(i);
    await Future.delayed(Duration(seconds: 1));
  }
}

LLMConfig _buildLLMConfigDto() {
  DotEnv env = DotEnv();
  env.load(['example/.env']);
  return LLMConfig(
    baseUrl: env["baseUrl"]!, apiKey: env["apiKey"]!, model: "gpt-4o-mini");
}

/// Use Prompt engineering to design SystemPrompt
/// https://platform.openai.com/docs/guides/prompt-engineering
String _buildSystemPrompt() {
  return 'just reply me for what I said to you, NO others.';
}

class AgentMessageHandlerImpl extends AgentMessageHandler {
  @override
  Future<ToolReturn> onFunctionCall(String sessionId, FunctionCall functionCall) async {
    // execute function and return result
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