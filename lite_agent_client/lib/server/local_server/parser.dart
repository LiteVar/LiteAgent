import 'dart:convert';

import 'package:get/get.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:opentool_dart/opentool_dart.dart';

import '../../models/local/message.dart';
import '../../utils/log_util.dart';

class MessageHandler {
  final List<ChatMessageModel> chatMessageList;
  final Map<String, ChatMessageModel> messageMap = {};
  Function(ChatMessageModel message)? onThoughtUpdate;
  Function(ChatMessageModel message)? onAgentReply;
  Function() onHandlerFinished;
  Map<String, String>? subAgentNameMap;

  MessageHandler({
    required this.chatMessageList,
    required this.onHandlerFinished,
    this.onThoughtUpdate,
    this.onAgentReply,
    this.subAgentNameMap,
  });

  ChatMessageModel newReceivedMessage(String messageId) {
    var receivedMessage = ChatMessageModel()
      ..sendRole = ChatRoleType.Agent
      ..isLoading = true;
    messageMap[messageId] = receivedMessage;
    return receivedMessage;
  }

  void handle(String mainAgentSessionId, String messageId, dynamic agentMessage) {
    if (agentMessage is! AgentMessageChunkDto && agentMessage is! AgentMessageDto) {
      return;
    }
    var chatMessage = messageMap[messageId];
    chatMessage ??= newReceivedMessage(messageId);
    try {
      // 处理 AgentMessageDto 类型的消息
      if (agentMessage is AgentMessageDto) {
        if (_isUserToAgent(agentMessage)) {
          _handleUserToAgent(chatMessage, mainAgentSessionId, agentMessage);
        } else if (_isReasoning(agentMessage)) {
          _handleReasoning(chatMessage, messageId, agentMessage);
        } else if (_isAgentCallSubAgentForTool(agentMessage)) {
          _handleAgentCallSubAgentForTool(chatMessage, messageId, agentMessage);
        } else if (_isClientTaskStatus(agentMessage)) {
          _handleTaskStatus(chatMessage, messageId, agentMessage);
        } else if (_isAgentUseTool(agentMessage)) {
          _handleAgentUseTool(chatMessage, agentMessage);
        } else if (_isToolCallBackToAgent(agentMessage)) {
          _handleToolCallBackToAgent(chatMessage, agentMessage);
        } else if (_isSubAgentCallBackToAgent(agentMessage)) {
          _handleSubAgentCallBackToAgent(chatMessage, agentMessage);
        } else if (_isAgentReflection(agentMessage)) {
          _handleAgentReflection(chatMessage, agentMessage);
        } else if (_isAgentToUser(agentMessage)) {
          _handleAgentToUser(chatMessage, mainAgentSessionId, agentMessage);
        }
      }
      // 处理 AgentMessageChunkDto 类型的消息
      else if (agentMessage is AgentMessageChunkDto) {
        if (_isAgentToUser(agentMessage)) {
          if (agentMessage.type == ReasoningMessageType.REASONING) {
            _handleReasoning(chatMessage, messageId, agentMessage);
          } else {
            _handleAgentToUser(chatMessage, mainAgentSessionId, agentMessage);
          }
        }
      }
      onHandlerFinished();
    } catch (e) {
      Log.e("Message handling error: ${e.toString()}");
    }
  }

  bool _isAgentCallSubAgentForTool(AgentMessageDto msg) =>
      msg.role == ToolRoleType.AGENT && msg.to == MultiAgentRoleType.SUBAGENT && msg.type == ToolMessageType.TOOL_CALLS;

  bool _isSubAgentCallBackToAgent(AgentMessageDto msg) =>
      msg.role == MultiAgentRoleType.SUBAGENT && msg.to == ToolRoleType.AGENT && msg.type == ToolMessageType.TOOL_RETURN;

  bool _isUserToAgent(AgentMessageDto msg) => msg.role == ToolRoleType.USER && msg.to == ToolRoleType.AGENT;

  bool _isClientTaskStatus(AgentMessageDto msg) =>
      msg.role == ToolRoleType.AGENT && msg.to == ToolRoleType.CLIENT && msg.type == AgentMessageType.TASK_STATUS;

  bool _isAgentUseTool(AgentMessageDto msg) =>
      msg.role == ToolRoleType.AGENT && msg.to == ToolRoleType.TOOL && msg.type == ToolMessageType.TOOL_CALLS;

  bool _isToolCallBackToAgent(AgentMessageDto msg) =>
      msg.role == ToolRoleType.TOOL && msg.to == ToolRoleType.AGENT && msg.type == ToolMessageType.TOOL_RETURN;

  bool _isAgentToUser(dynamic msg) => msg.role == ToolRoleType.AGENT && msg.to == ToolRoleType.USER;

  bool _isAgentReflection(AgentMessageDto msg) => msg.role == ToolRoleType.REFLECTION && msg.to == ToolRoleType.AGENT;

  bool _isReasoning(dynamic msg) => msg.reasoningContent != null && msg.reasoningContent!.trimmed().isNotEmpty;

  void _handleUserToAgent(ChatMessageModel chatMessage, mainAgentSessionId, AgentMessageDto msg) {
    //when one message has two sessionIds, it means that the message is from a subagent.
    if (mainAgentSessionId != msg.sessionId) {
      final childMessage = ChatMessageModel()
        ..sendRole = ChatRoleType.SubAgent
        ..roleName = subAgentNameMap?[msg.sessionId] ?? ""
        ..taskId = msg.taskId
        ..isLoading = true;

      if (msg.type == AgentMessageType.CONTENT_LIST) {
        List<String> contentList = (msg.content as List<dynamic>).map((json) => jsonEncode(json)).toList();
        for (var content in contentList) {
          Map<String, dynamic> json = jsonDecode(content);
          if (childMessage.message.isEmpty) {
            childMessage.message = "${json["message"]}";
          } else {
            childMessage.message = "\n${json["message"]}";
          }
        }
      } else if (msg.type == AgentMessageType.TEXT) {
        childMessage.message = msg.content as String;
      }

      chatMessage.subMessages ??= [];
      chatMessage.subMessages?.add(childMessage);
    } else {
      chatMessage.taskId = msg.taskId;
      if (!chatMessageList.contains(chatMessage)) {
        chatMessageList.add(chatMessage);
      }
    }
  }

  void _handleReasoning(ChatMessageModel chatMessage, String messageId, dynamic agentMessage) {
    var message = getTargetMessage(chatMessage, agentMessage.taskId);

    if (agentMessage is AgentMessageDto) {
      if (message != null) {
        final reasoningMessage = ChatMessageModel()
          ..sendRole = ChatRoleType.Reasoning
          ..message = (agentMessage.reasoningContent ?? "").trimmed()
          ..taskId = agentMessage.taskId
          ..isLoading = false;

        message.subMessages ??= [];
        message.subMessages?.add(reasoningMessage);
      }
    } else if (agentMessage is AgentMessageChunkDto) {
      if (message != null) {
        ChatMessageModel? reasoningMessage;
        reasoningMessage = message.subMessages?.last;
        var isReasoning = reasoningMessage?.sendRole == ChatRoleType.Reasoning;
        if (!isReasoning) {
          reasoningMessage = ChatMessageModel()
            ..sendRole = ChatRoleType.Reasoning
            ..taskId = agentMessage.taskId;

          message.subMessages ??= [];
          message.subMessages?.add(reasoningMessage);
        }
        String content = agentMessage.part as String;
        reasoningMessage?.message += content;
      }
    }

    onThoughtUpdate?.call(chatMessage);
  }

  void _handleAgentCallSubAgentForTool(ChatMessageModel chatMessage, String messageId, AgentMessageDto agentMessage) {
    List<dynamic> originalFunctionCallList = agentMessage.content as List<dynamic>;
    List<FunctionCall> functionCallList = originalFunctionCallList.map((dynamic json) => FunctionCall.fromJson(json)).toList();
    for (var functionCall in functionCallList) {
      if (functionCall.id.isNumericOnly) {
        subAgentNameMap ??= {};
        subAgentNameMap?[functionCall.id] = functionCall.name;
      }
    }
  }

  void _handleTaskStatus(ChatMessageModel chatMessage, String messageId, AgentMessageDto msg) {
    Map<String, dynamic> json = msg.content;
    bool isDone = false;
    String errorMessage = "";
    if (json["status"] == "start" && json["description"] != null) {
      if (json["description"]["code"] != 200) {
        isDone = true;
        if (json["description"]["code"] == 409) {
          //for reject mode
          errorMessage = "There are still running tasks, please wait";
        } else {
          String string = json["description"]["message"];
          errorMessage = string.isNotEmpty ? string : "服务暂停,请再试";
        }
        final childMessage = ChatMessageModel()
          ..sendRole = ChatRoleType.Agent
          ..message = errorMessage
          ..taskId = msg.taskId
          ..isLoading = false;

        chatMessageList.add(childMessage);
        return;
      }
    } else if (json["status"] == "done") {
      isDone = true;
    } else if (json["status"] == "stop") {
      isDone = true;
      errorMessage = "服务暂停,请再试";
    } else if (json["status"] == "exception") {
      isDone = true;
      errorMessage = jsonEncode(json["description"]);
      if (json["description"]["code"] == 409) {
        //for reject mode
        errorMessage = "The device is busy, please wait a moment";
      } else if (json["description"]["code"] == 500 || json["description"]["code"] == 503) {
        errorMessage = "服务暂时无法响应。请稍后再试。如果问题持续存在，请联系管理员。";
      } else if (json["description"]["error"] != null) {
        String string = json["description"]["error"];
        if (string.isJson()) {
          Map<String, dynamic> errorJson = jsonDecode(string);
          if (errorJson["code"] == 500) {
            errorMessage = "服务暂时无法响应。请稍后再试。如果问题持续存在，请联系管理员。";
          }
        }
      }
    }
    if (isDone) {
      ChatMessageModel? targetMessage = getTargetMessage(chatMessage, msg.taskId);
      if (targetMessage != null) {
        targetMessage.isLoading = false;
        if (errorMessage.isNotEmpty) {
          if (targetMessage.sendRole == ChatRoleType.Agent) {
            targetMessage.message = errorMessage;
          } else {
            targetMessage.receivedMessage = errorMessage;
            onThoughtUpdate?.call(chatMessage);
          }
        }
      }
    }
  }

  void _handleAgentUseTool(ChatMessageModel chatMessage, AgentMessageDto msg) {
    List<dynamic> originalFunctionCallList = msg.content as List<dynamic>;
    List<FunctionCall> functionCallList = originalFunctionCallList.map((dynamic json) => FunctionCall.fromJson(json)).toList();
    for (var functionCall in functionCallList) {
      var message = getTargetMessage(chatMessage, msg.taskId);
      if (message != null) {
        ChatMessageModel subMessage = ChatMessageModel()
          ..sendRole = ChatRoleType.Tool
          ..taskId = functionCall.id
          ..roleName = functionCall.name
          ..isLoading = true
          ..message = jsonEncode(functionCall.arguments);

        message.subMessages ??= [];
        message.subMessages?.add(subMessage);

        onThoughtUpdate?.call(chatMessage);
      }
    }
  }

  void _handleToolCallBackToAgent(ChatMessageModel chatMessage, AgentMessageDto msg) {
    ToolReturn toolReturn = ToolReturn.fromJson(msg.content);
    var message = getTargetMessage(chatMessage, toolReturn.id);
    if (message != null) {
      if (message.sendRole == ChatRoleType.Tool) {
        var result = toolReturn.result["body"];
        result ??= toolReturn.result.toString();
        message.isLoading = false;
        message.receivedMessage = result;
      }
      onThoughtUpdate?.call(chatMessage);
    }
  }

  void _handleSubAgentCallBackToAgent(ChatMessageModel chatMessage, AgentMessageDto agentMessage) {
    ToolReturn toolReturn = ToolReturn.fromJson(agentMessage.content);
    var message = getTargetMessage(chatMessage, agentMessage.taskId);
    if (message != null) {
      if (message.sendRole == ChatRoleType.SubAgent) {
        if (toolReturn.id.isNumericOnly) {
          message.roleName = subAgentNameMap?[toolReturn.id] ?? "";
          subAgentNameMap?.remove(toolReturn.id);
        }
      }
      onThoughtUpdate?.call(chatMessage);
    }
  }

  void _handleAgentToUser(ChatMessageModel mainMessage, String mainSessionId, dynamic agentMessage) {
    if (agentMessage.type == ToolMessageType.TEXT) {
      //mainMessage
      if (mainSessionId == agentMessage.sessionId) {
        if (agentMessage is AgentMessageDto) {
          mainMessage.message = agentMessage.content as String;
          if (mainMessage.message.isNotEmpty) {
            onAgentReply?.call(mainMessage);
          }
        } else if (agentMessage is AgentMessageChunkDto) {
          String content = agentMessage.part as String;
          mainMessage.message += content;
        }
      } else {
        //subMessage
        var subMessage = getTargetMessage(mainMessage, agentMessage.taskId);
        if (subMessage != null) {
          if (agentMessage is AgentMessageDto) {
            subMessage.receivedMessage = agentMessage.content as String;
          } else if (agentMessage is AgentMessageChunkDto) {
            String content = agentMessage.part as String;
            subMessage.receivedMessage = (mainMessage.receivedMessage ?? "") + content;
          }
          onThoughtUpdate?.call(subMessage);
        }
      }
    }
  }

  void _handleAgentReflection(ChatMessageModel chatMessage, AgentMessageDto msg) {
    ReflectionDto reflectionDto = ReflectionDto.fromJson(msg.content);
    // ignore tool calls
    // if (reflectionDto.messageScore.messageType == ToolMessageType.TOOL_CALLS) {
    //   return;
    // }
    String reflectionResult = "";
    for (var reflectScore in reflectionDto.messageScore.reflectScoreList) {
      if (reflectionResult.isNotEmpty) {
        reflectionResult += "\n${jsonEncode(reflectScore)}";
      } else {
        reflectionResult += jsonEncode(reflectScore);
      }
    }

    String reflectionMessageContent =
        "原输入:${reflectionDto.messageScore.contentList.first.message}，原输出:${reflectionDto.messageScore.message}";

    var message = getTargetMessage(chatMessage, msg.taskId);
    if (message != null) {
      ChatMessageModel reflectionMessage = ChatMessageModel()
        ..sendRole = ChatRoleType.Reflection
        ..taskId = msg.taskId
        ..message = reflectionMessageContent
        ..receivedMessage = reflectionResult;

      message.subMessages ??= [];
      message.subMessages?.add(reflectionMessage);

      onThoughtUpdate?.call(chatMessage);
    }
  }

  ChatMessageModel? getTargetMessage(ChatMessageModel mainMessage, String taskId) {
    if (mainMessage.taskId == taskId) {
      return mainMessage;
    }
    var subAgentMessages = mainMessage.subMessages ?? [];
    for (var message in subAgentMessages) {
      if (message.taskId == taskId) {
        return message;
      }
      var subMessage = getTargetMessage(message, taskId);
      if (subMessage != null) {
        return subMessage;
      }
    }
    return null;
  }

  void removeMessage(String messageId) {
    var message = messageMap[messageId];
    if (message != null) {
      message.isLoading = false;
      for (var subMessage in message.subMessages ?? []) {
        subMessage.isLoading = false;
      }
      onThoughtUpdate?.call(message);
    }
    messageMap.remove(messageId);
  }

  dispose() {
    onThoughtUpdate = null;
    onAgentReply = null;
    subAgentNameMap = null;
    for (var key in messageMap.keys) {
      removeMessage(key);
    }
    messageMap.clear();
  }
}
