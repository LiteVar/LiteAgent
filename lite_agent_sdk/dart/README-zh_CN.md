# LiteAgent SDK for Dart

[English](README.md) · 中文

LiteAgent的Dart SDK，用于Dart和Flutter应用程序中与LiteAgent进行交互。

## 功能

- 初始化Agent的会话
- 向Agent发送客户端消息
- 订阅Agent消息，包括：Agent消息、逐个吐字时的chunk消息、SSE的Done和Error、Function Call的回调请求
- 发送Function Call的Callback结果
- 停止当前会话
- 清除当前会话

## 安装

在 `pubspec.yaml` 文件中增加如下依赖：

```yaml
dependencies:
  liteagent_sdk_dart: ^0.1.0
```

并运行:

```bash
dart pub get
```

## 使用

- 实现AgentMessageHandler，用以订阅Agent各类推送消息

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