import 'package:opentool_dart/opentool_dart.dart';
import 'model/model.dart';

abstract class AgentMessageHandler {

  /// When Agent Call Native Function, will callback onFunctionCall
  Future<ToolReturn> onFunctionCall(String sessionId, FunctionCall functionCall) async {
    return ToolReturn(id: functionCall.id, result: FunctionNotSupportedException(functionName: functionCall.name).toJson());
  }

  /// When SSE connection receive a message, will callback onMessage()
  Future<void> onMessage(String sessionId, AgentMessage agentMessage);

  /// When SSE connection receive a chunk, will callback onChunk()
  Future<void> onChunk(String sessionId, AgentMessageChunk agentMessageChunk);

  /// When SSE connection DONE, will callback onDone()
  Future<void> onDone();

  /// When SSE Error, will callback onError(e)
  Future<void> onError(Exception e);

}