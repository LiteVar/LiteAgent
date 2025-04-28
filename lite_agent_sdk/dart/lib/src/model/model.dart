import 'package:json_annotation/json_annotation.dart';

part 'model.g.dart';

@JsonSerializable()
class Session {
  String sessionId;

  Session({required this.sessionId});

  factory Session.fromJson(Map<String, dynamic> json) => _$SessionFromJson(json);

  Map<String, dynamic> toJson() => _$SessionToJson(this);
}

@JsonSerializable()
class SessionName extends Session {

  @JsonKey(includeIfNull: false)
  String? name; // OpenAI: The name of the function to be called. Must be a-z, A-Z, 0-9, or contain underscores and dashes, with a maximum length of 64.

  SessionName({required super.sessionId, String? name});

  factory SessionName.fromJson(Map<String, dynamic> json) => _$SessionNameFromJson(json);

  @override
  Map<String, dynamic> toJson() => _$SessionNameToJson(this);
}

@JsonSerializable(explicitToJson: true)
class SimpleCapability {
  LLMConfig llmConfig;
  String systemPrompt;

  SimpleCapability({
    required this.llmConfig,
    required this.systemPrompt
  });

  factory SimpleCapability.fromJson(Map<String, dynamic> json) => _$SimpleCapabilityFromJson(json);

  Map<String, dynamic> toJson() => _$SimpleCapabilityToJson(this);
}

enum PipelineStrategyType {
  parallel,
  serial,
  reject,
}

@JsonSerializable(explicitToJson: true)
class Capability extends SimpleCapability {
  @JsonKey(includeIfNull: false) List<OpenSpec>? openSpecList;
  @JsonKey(includeIfNull: false) ClientOpenTool? clientOpenTool;
  @JsonKey(includeIfNull: false) List<SessionName>? sessionList;
  @JsonKey(includeIfNull: false) List<ReflectPrompt>? reflectPromptList;
  int timeoutSeconds;
  PipelineStrategyType taskPipelineStrategy;
  @JsonKey(includeIfNull: false) PipelineStrategyType? toolPipelineStrategy;

  Capability({
    required super.llmConfig,
    required super.systemPrompt,
    this.openSpecList,
    this.clientOpenTool,
    this.sessionList,
    this.reflectPromptList,
    this.timeoutSeconds = 3600,
    this.taskPipelineStrategy  = PipelineStrategyType.parallel,
    this.toolPipelineStrategy
  });

  factory Capability.fromJson(Map<String, dynamic> json) => _$CapabilityFromJson(json);

  @override
  Map<String, dynamic> toJson() => _$CapabilityToJson(this);
}

enum Protocol {
  openapi,
  openmodbus,
  jsonrpcHttp,
  opentool,
  serialport,
}

@JsonSerializable(explicitToJson: true)
class OpenSpec {
  String openSpec;

  @JsonKey(includeIfNull: false)
  ApiKey? apiKey;
  Protocol protocol;

  @JsonKey(includeIfNull: false)
  String? openToolId; //When protocol is open tool, this is the tool id

  OpenSpec({required this.openSpec, this.apiKey, required this.protocol, this.openToolId});

  factory OpenSpec.fromJson(Map<String, dynamic> json) => _$OpenSpecFromJson(json);

  Map<String, dynamic> toJson() => _$OpenSpecToJson(this);
}

@JsonSerializable()
class LLMConfig {
  String baseUrl;
  String apiKey;
  String model;
  double temperature;
  int maxTokens;
  double topP;

  LLMConfig({
    required this.baseUrl,
    required this.apiKey,
    required this.model,
    this.temperature = 0.0,
    this.maxTokens = 4096,
    this.topP = 1.0
  });

  factory LLMConfig.fromJson(Map<String, dynamic> json) => _$LLMConfigFromJson(json);

  Map<String, dynamic> toJson() => _$LLMConfigToJson(this);
}

@JsonSerializable(explicitToJson: true)
class AgentMessage {
  String sessionId;
  String taskId;
  String role;
  String to;
  String type;
  dynamic content;

  @JsonKey(includeIfNull: false) Completions? completions; //When role is llm, this is current llm calling token usage

  DateTime createTime;

  AgentMessage({
    required this.sessionId,
    required this.taskId,
    required this.role,
    required this.to,
    required this.type,
    required this.content,
    this.completions,
    required this.createTime
  });

  factory AgentMessage.fromJson(Map<String, dynamic> json) => _$AgentMessageFromJson(json);

  Map<String, dynamic> toJson() => _$AgentMessageToJson(this);

}

@JsonSerializable(explicitToJson: true)
class Completions {
  TokenUsage usage;

  /// When role is llm, this is current llm calling token usage
  String id;

  /// When role is llm, this is current /chat/completions return message id
  String model;

  Completions({required this.usage, required this.id, required this.model});

  factory Completions.fromJson(Map<String, dynamic> json) => _$CompletionsFromJson(json);

  Map<String, dynamic> toJson() => _$CompletionsToJson(this);

}

@JsonSerializable()
class TokenUsage {
  final int promptTokens;
  final int completionTokens;
  final int totalTokens;

  TokenUsage({
    required this.promptTokens,
    required this.completionTokens,
    required this.totalTokens
  });

  factory TokenUsage.fromJson(Map<String, dynamic> json) => _$TokenUsageFromJson(json);

  Map<String, dynamic> toJson() => _$TokenUsageToJson(this);

}

enum ApiKeyType { basic, bearer }

@JsonSerializable()
class ApiKey {
  ApiKeyType type;
  String apiKey;

  ApiKey({required this.type, required this.apiKey});

  factory ApiKey.fromJson(Map<String, dynamic> json) => _$ApiKeyFromJson(json);

  Map<String, dynamic> toJson() => _$ApiKeyToJson(this);
}

@JsonSerializable(explicitToJson: true)
class UserTask {
  @JsonKey(includeIfNull: false) String? taskId;
  List<Content> content;
  @JsonKey(includeIfNull: true) bool? stream;

  UserTask({this.taskId, required this.content, this.stream});

  factory UserTask.fromJson(Map<String, dynamic> json) {
    if(json["taskId"] != null && (json["taskId"] as String).length > 36) {
      throw FormatException("taskId length should not more then 36");
    }
    return _$UserTaskFromJson(json);
  }

  Map<String, dynamic> toJson() => _$UserTaskToJson(this);
}

// enum UserMessageType { text, imageUrl }
// class UserMessageType {
//   static final String text = "text";
//   static final String imageUrl = "imageUrl";
// }
//
// @JsonSerializable()
// class UserMessage {
//   String type;
//   String message;
//
//   UserMessage({required this.type, required this.message});
//
//   factory UserMessage.fromJson(Map<String, dynamic> json) => _$UserMessageFromJson(json);
//
//   Map<String, dynamic> toJson() => _$UserMessageToJson(this);
// }

@JsonSerializable()
class SessionTask {
  String sessionId;

  @JsonKey(includeIfNull: false)
  String? taskId;

  SessionTask({required this.sessionId, this.taskId});

  factory SessionTask.fromJson(Map<String, dynamic> json) => _$SessionTaskFromJson(json);

  Map<String, dynamic> toJson() => _$SessionTaskToJson(this);
}

@JsonSerializable()
class ReflectScore {
  int score;

  @JsonKey(includeIfNull: false)
  String? description;
  ReflectScore({required this.score, this.description});

  factory ReflectScore.fromJson(Map<String, dynamic> json) => _$ReflectScoreFromJson(json);

  Map<String, dynamic> toJson() => _$ReflectScoreToJson(this);
}

@JsonSerializable(explicitToJson: true)
class MessageScore {
  List<Content> content;
  String messageType; //Follow AgentMessage.type
  String message;
  List<ReflectScore> reflectScoreList;
  MessageScore({required this.content, required this.messageType, required this.message, required this.reflectScoreList});

  factory MessageScore.fromJson(Map<String, dynamic> json) => _$MessageScoreFromJson(json);

  Map<String, dynamic> toJson() => _$MessageScoreToJson(this);
}

@JsonSerializable(explicitToJson: true)
class Reflection {
  final bool isPass;
  final MessageScore messageScore;
  final int passScore;
  final int count;
  final int maxCount;

  Reflection({
    required this.isPass,
    required this.messageScore,
    required this.passScore,
    required this.count,
    required this.maxCount,
  });

  factory Reflection.fromJson(Map<String, dynamic> json) => _$ReflectionFromJson(json);

  Map<String, dynamic> toJson() => _$ReflectionToJson(this);
}

@JsonSerializable()
class TaskStatus {
  String status;
  String taskId;

  @JsonKey(includeIfNull: false)
  Map<String, dynamic>? description;

  TaskStatus({required this.status, required this.taskId, this.description});

  factory TaskStatus.fromJson(Map<String, dynamic> json) => _$TaskStatusFromJson(json);

  Map<String, dynamic> toJson() => _$TaskStatusToJson(this);
}

// enum ContentType {
//   text,
//   image_url
// }

class ContentType {
  static final String text = "text";
  static final String imageUrl = "imageUrl";
}

@JsonSerializable()
class Content {
  String type;
  String message;

  Content({required this.type, required this.message});

  factory Content.fromJson(Map<String, dynamic> json) => _$ContentFromJson(json);

  Map<String, dynamic> toJson() => _$ContentToJson(this);
}

@JsonSerializable(explicitToJson: true)
class ReflectPrompt {
  LLMConfig llmConfig;
  String prompt;

  ReflectPrompt({required this.llmConfig, required this.prompt});

  factory ReflectPrompt.fromJson(Map<String, dynamic> json) => _$ReflectPromptFromJson(json);

  Map<String, dynamic> toJson() => _$ReflectPromptToJson(this);
}

@JsonSerializable(explicitToJson: true)
class AgentMessageChunk {
  String sessionId;
  String taskId;
  String role;
  String to;
  String type;
  dynamic part;
  DateTime createTime;

  AgentMessageChunk({
    required this.sessionId,
    required this.taskId,
    required this.role,
    required this.to,
    required this.type,
    required this.part,
    required this.createTime
  });

  factory AgentMessageChunk.fromJson(Map<String, dynamic> json) => _$AgentMessageChunkFromJson(json);

  Map<String, dynamic> toJson() => _$AgentMessageChunkToJson(this);

}

@JsonSerializable()
class AgentId {
  String agentId;
  AgentId({required this.agentId});

  factory AgentId.fromJson(Map<String, dynamic> json) => _$AgentIdFromJson(json);

  Map<String, dynamic> toJson() => _$AgentIdToJson(this);
}

@JsonSerializable(explicitToJson: true)
class Agent {
  String name;
  Capability capability;
  Agent({required this.name, required this.capability});

  factory Agent.fromJson(Map<String, dynamic> json) => _$AgentFromJson(json);

  Map<String, dynamic> toJson() => _$AgentToJson(this);
}

@JsonSerializable(explicitToJson: true)
class AgentInfo extends AgentId {
  String name;
  Capability capability;
  AgentInfo({required super.agentId, required this.name, required this.capability});

  factory AgentInfo.fromJson(Map<String, dynamic> json) => _$AgentInfoFromJson(json);

  @override
  Map<String, dynamic> toJson() => _$AgentInfoToJson(this);
}

@JsonSerializable(explicitToJson: true)
class SessionAgentMessage {
  String sessionId;
  AgentMessage agentMessage;
  SessionAgentMessage({required this.sessionId, required this.agentMessage});

  factory SessionAgentMessage.fromJson(Map<String, dynamic> json) => _$SessionAgentMessageFromJson(json);

  Map<String, dynamic> toJson() => _$SessionAgentMessageToJson(this);
}

@JsonSerializable()
class Version {
  late String version;

  Version({required this.version});

  factory Version.fromJson(Map<String, dynamic> json) => _$VersionFromJson(json);

  Map<String, dynamic> toJson() => _$VersionToJson(this);
}

@JsonSerializable()
class OpenToolInfo {
  late String openToolId;
  late String description;

  OpenToolInfo({required this.openToolId, required this.description});

  factory OpenToolInfo.fromJson(Map<String, dynamic> json) => _$OpenToolInfoFromJson(json);

  Map<String, dynamic> toJson() => _$OpenToolInfoToJson(this);
}

class SSEEventType {
  static const String MESSAGE = "message";
  static const String CHUNK = "chunk";
  static const String FUNCTION_CALL = "functionCall";
}

@JsonSerializable(explicitToJson: true)
class ClientOpenTool {
  String opentool;
  int? timeout;

  ClientOpenTool({required this.opentool, this.timeout});

  factory ClientOpenTool.fromJson(Map<String, dynamic> json) => _$ClientOpenToolFromJson(json);

  Map<String, dynamic> toJson() => _$ClientOpenToolToJson(this);
}

class AgentRoleType {
  static const String SYSTEM = "developer"; // system prompt
  static const String USER = "user"; // user
  static const String AGENT = "agent"; // agent
  static const String LLM = "assistant"; // llm
  static const String TOOL = "tool"; // external tools
  static const String CLIENT = "client"; // external caller
}

class AgentMessageType {
  static const String TEXT = "text"; //String
  static const String IMAGE_URL = "imageUrl"; //String
  // static const String DISPATCH = "dispatch"; //Dispatch
  static const String TOOL_CALLS = "toolCalls"; //List<FunctionCall>
  static const String TOOL_RETURN = "toolReturn"; //ToolReturn
  static const String CONTENT_LIST = "contentList"; //List<Content>
  static const String REFLECTION = "reflection"; //Reflection
  static const String TASK_STATUS = "taskStatus"; //TaskStatus
  static const String FUNCTION_CALL = "functionCall"; //FunctionCall
}

class TaskStatusType {
  static const String START = "start";
  static const String STOP = "stop";
  static const String DONE = "done";
  static const String EXCEPTION = "exception";
  static const String TOOLS_START = "toolsStart";
  static const String TOOLS_DONE = "toolsDone";
}