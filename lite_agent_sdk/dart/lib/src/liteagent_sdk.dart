import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:opentool_dart/opentool_dart.dart';
import 'agent_message_handler.dart';
import 'api/http_service.dart';
import 'api/sse_service.dart';
import 'model/model.dart';

class LiteAgentSDK {
  late HttpService httpService;
  late SseService sseService;
  // late Session session;

  LiteAgentSDK({required String baseUrl, String? apiKey}) {
    Dio dio = Dio();
    dio.options.baseUrl = baseUrl;
    if(apiKey != null) dio.options.headers['Authorization'] = 'Bearer $apiKey';
    httpService = HttpService(dio);
    sseService = SseService(baseUrl, apiKey);
  }

  Future<Version> getVersion() async {
    Version version = await httpService.getVersion();
    return version;
  }

  Future<Session> initSession(String agentId) async {
    return await httpService.initSession(agentId: agentId);
  }

  Future<void> chat(Session session, UserTask userTask, AgentMessageHandler agentMessageHandler, {bool stream = true}) async {
    Stream<String> stream = await sseService.chat(session.sessionId, userTask);
    // Stream<String> stringStream = stream.map((List<int> bytes){
    //   print(bytes);
    //   return utf8.decode(bytes);
    // });
    stream.listen(
        (String data) async {
          final eventRegex = RegExp(r'event:(\w+)\ndata:(.*?)\n\n');
          final matches = eventRegex.allMatches(data);

          for (var match in matches) {
            final eventName = match.group(1);
            final eventData = match.group(2);

            if (eventName == SSEEventType.MESSAGE) {
              AgentMessage agentMessage = AgentMessage.fromJson(jsonDecode(eventData!));
              agentMessageHandler.onMessage(session.sessionId, agentMessage);
            } else if (eventName == SSEEventType.CHUNK) {
              AgentMessageChunk agentMessageChunk = AgentMessageChunk.fromJson(jsonDecode(eventData!));
              agentMessageHandler.onChunk(session.sessionId, agentMessageChunk);
            } else if (eventName == SSEEventType.FUNCTION_CALL) {
              AgentMessage agentMessage = AgentMessage.fromJson(jsonDecode(eventData!));
              FunctionCall functionCall = FunctionCall.fromJson(agentMessage.content);
              ToolReturn toolReturn = await agentMessageHandler.onFunctionCall(session.sessionId, functionCall);
              await httpService.callback(sessionId: session.sessionId, toolReturn: toolReturn);
            }
          }
        },
      onDone: (){
        agentMessageHandler.onDone();
      },
      onError: (e){
        agentMessageHandler.onError(e);
      }
    );
  }

  // Future<void> callback(Session session, ToolReturn toolReturn) async {
  //   await httpService.callback(sessionId: session.sessionId, toolReturn: toolReturn);
  // }

  Future<List<AgentMessage>> getHistory(Session session) async {
    return await httpService.getHistory(sessionId: session.sessionId);
  }

  Future<void> stop(Session session, {String? taskId}) async {
    await httpService.stop(sessionId: session.sessionId, taskId: taskId);
  }

  Future<void> clear(Session session) async {
    await httpService.clear(sessionId: session.sessionId);
  }

  Future<List<AgentInfo>> listAgent() async {
    return await httpService.listAgent();
  }

  Future<AgentInfo> getAgent(String agentId) async {
    return await httpService.getAgent(agentId: agentId);
  }

  Future<AgentId> createAgent(Agent agent) async {
    return await httpService.createAgent(agent: agent);
  }

  Future<AgentId> updateAgent(String agentId, Agent agent) async {
    return await httpService.updateAgent(agentId: agentId, agent: agent);
  }

  Future<AgentId> deleteAgent(String agentId) async {
    return await httpService.deleteAgent(agentId: agentId);
  }
}
