# LiteAgent SDK for Dart

English · [中文](README-zh_CN.md)

The LiteAgent Dart SDK is used for interacting with LiteAgent in Dart and Flutter applications.

## Features

- Initialize an Agent session
- Send client messages to the Agent
- Subscribe to Agent messages, including: Agent messages, chunk messages during word-by-word typing, SSE Done and Error, and Function Call callback requests
- Send Function Call callback results
- Stop the current session
- Clear the current session

## Installation

Add the following dependency in your `pubspec.yaml` file:

```yaml
dependencies:
  liteagent_sdk_dart: ^0.1.1
```

Then run:

```bash
dart pub get
```

## Usage

- Implement AgentMessageHandler to subscribe to various Agent push messages

```dart
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
```