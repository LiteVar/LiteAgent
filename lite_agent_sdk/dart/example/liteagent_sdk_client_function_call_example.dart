import 'dart:convert';
import 'dart:io';
import 'package:dotenv/dotenv.dart';
import 'package:liteagent_sdk_dart/liteagent_sdk_dart.dart';
import 'package:opentool_dart/opentool_dart.dart';
import 'mock/utils/mock_util.dart';

String baseUrl = "http://127.0.0.1:9527/api/v1";

String userPrompt = "Help me storage text `hello1`.";

Future<void> main() async {
  LiteAgentSDK liteAgent = LiteAgentSDK(baseUrl: baseUrl);
  Version version = await liteAgent.getVersion();

  Capability capabilityDto = Capability(
      llmConfig: _buildLLMConfigDto(),
      systemPrompt: _buildSystemPrompt(),
      clientOpenTool: await _buildClientOpenTool(),
      timeoutSeconds: 20
  );

  Agent agentDto = Agent(name: "Storage Manager", capability: capabilityDto);

  AgentId agentIdDto = await liteAgent.createAgent(agentDto);

  List<AgentInfo> agentInfoDtoList = await liteAgent.listAgent();

  agentInfoDtoList.forEach((agentInfoDto) => print(agentInfoDto.toJson()));

  AgentMessageHandler agentMessageHandler = AgentMessageHandlerImpl();

  Session session = await liteAgent.initSession(agentIdDto.agentId);

  UserTask userTaskDto = UserTask(content: [Content(type: ContentType.text, message: userPrompt)]);

  print(jsonEncode(userTaskDto.toJson()));

  await liteAgent.chat(session, userTaskDto, agentMessageHandler);

  await sleep(100);

  List<AgentMessage> agentMessageDtoList = await liteAgent.getHistory(session);

  agentMessageDtoList.forEach((agentMessageDto) => print(agentMessageDto.toJson()));

  await sleep(3);

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
  baseUrl: env["baseUrl"]!, apiKey: env["apiKey"]!, model: "qwq-32b");
  // baseUrl: env["baseUrl"]!, apiKey: env["apiKey"]!, model: "deepseek-reasoner");
  // baseUrl: env["baseUrl"]!, apiKey: env["apiKey"]!, model: "deepseek-chat");
  // baseUrl: env["baseUrl"]!, apiKey: env["apiKey"]!, model: "deepseek-r1-distill-qwen-14b@8bit");
  // baseUrl: env["baseUrl"]!, apiKey: env["apiKey"]!, model: "gpt-4o-mini");
}

/// Use Prompt engineering to design SystemPrompt
/// https://platform.openai.com/docs/guides/prompt-engineering
String _buildSystemPrompt() {
  return 'A storage management tool that knows how to add, delete, modify, and query my texts.';
}

Future<ClientOpenTool> _buildClientOpenTool() async {
  String openToolFolder = "${Directory.current.path}${Platform.pathSeparator}example${Platform.pathSeparator}mock${Platform.pathSeparator}utils${Platform.pathSeparator}";
  String openToolFileName = "mock-tool.json";
  String jsonPath = openToolFolder + openToolFileName;
  File file = File(jsonPath);
  String opentool = await file.readAsString();
  return ClientOpenTool(opentool: opentool);
}

class AgentMessageHandlerImpl extends AgentMessageHandler {
  MockUtil mockAPI = MockUtil();
  @override
  Future<ToolReturn> onFunctionCall(String sessionId, FunctionCall functionCall) async {
    print(functionCall.toJson().toString());
    String functionName = functionCall.name;
    ToolReturn toolReturn;
    if(functionName == "count") {
      int count = mockAPI.count();
      toolReturn = ToolReturn(id: functionCall.id, result: {"count": count});
    } else if(functionName == "create") {
      String text = functionCall.parameters["text"] as String;
      int id = mockAPI.create(text);
      toolReturn = ToolReturn(id: functionCall.id, result: {"id": id});
    } else if(functionName == "read") {
      int id = functionCall.parameters["id"] as int;
      String text = mockAPI.read(id);
      toolReturn = ToolReturn(id: functionCall.id, result: {"text": text});
    } else if(functionName == "update") {
      int id = functionCall.parameters["id"] as int;
      String text = functionCall.parameters["text"] as String;
      mockAPI.update(id, text);
      toolReturn = ToolReturn(id: functionCall.id, result: {"result": "Update successfully."});
    } else if(functionName == "delete") {
      int id = functionCall.parameters["id"] as int;
      mockAPI.delete(id);
      toolReturn = ToolReturn(id: functionCall.id, result: {"result": "Delete successfully."});
    } else {
      toolReturn = ToolReturn(id: functionCall.id, result: FunctionNotSupportedException(functionName: functionName).toJson());
    }
    print(toolReturn.toJson().toString());
    return toolReturn;
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