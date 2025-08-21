import 'dart:convert';
import 'dart:io';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:opentool_dart/opentool_dart.dart';

void listen(String sessionId, AgentMessageDto agentMessageDto) {
  String system = "🖥DEVEL ";
  String user = "👤USER  ";
  String agent = "🤖AGENT ";
  String subAgent = "👾SUBAGT";
  String llm = "💡ASSIST";
  String tool = "🔧TOOL  ";
  String client = "🔗CLIENT";
  String reflection = "🎯REFLEC";

  String message = "";
  if (agentMessageDto.type == ToolMessageType.TEXT)
    message = agentMessageDto.content as String;
  if (agentMessageDto.type == ToolMessageType.IMAGE_URL)
    message = agentMessageDto.content as String;
  if (agentMessageDto.type == ToolMessageType.TOOL_CALLS) {
    List<Map<String, dynamic>> functionCallList = (agentMessageDto.content as List<dynamic>).map((dynamic functionCall) => (functionCall as Map<String, dynamic>)).toList();
    message = jsonEncode(functionCallList);
  }
  if (agentMessageDto.type == ToolMessageType.TOOL_RETURN) {
    message = jsonEncode(agentMessageDto.content);
  };
  if (agentMessageDto.type == AgentMessageType.CONTENT_LIST) {
    List<Map<String, dynamic>> contentList = (agentMessageDto.content as List<dynamic>).map((dynamic content) => (content as Map<String, dynamic>)).toList();
    message = jsonEncode(contentList);
  }
  if (agentMessageDto.type == AgentMessageType.TASK_STATUS) {
    message = jsonEncode(agentMessageDto.content);
  }
  if (agentMessageDto.type == TextMessageType.REFLECTION) {
    message = jsonEncode(agentMessageDto.content);
  } if (agentMessageDto.type == ToolMessageType.FUNCTION_CALL) {
    message = jsonEncode(agentMessageDto.content);
  }

  String  role = "";
  if (agentMessageDto.role == ToolRoleType.DEVELOPER) {
    role = system;
    message = "\n$message";
  }
  if (agentMessageDto.role == ToolRoleType.USER)  role = user;
  if (agentMessageDto.role == ToolRoleType.AGENT)  role = agent;
  if (agentMessageDto.role == MultiAgentRoleType.SUBAGENT)  role = subAgent;
  if (agentMessageDto.role == ToolRoleType.ASSISTANT)  role = llm;
  if (agentMessageDto.role == ToolRoleType.TOOL)  role = tool;
  if (agentMessageDto.role == ToolRoleType.CLIENT)  role = client;
  if (agentMessageDto.role == ToolRoleType.REFLECTION)  role = reflection;

  String to = "";
  if (agentMessageDto.to == ToolRoleType.DEVELOPER) to = system;
  if (agentMessageDto.to == ToolRoleType.USER) to = user;
  if (agentMessageDto.to == ToolRoleType.AGENT) to = agent;
  if (agentMessageDto.to == MultiAgentRoleType.SUBAGENT) to = subAgent;
  if (agentMessageDto.to == ToolRoleType.ASSISTANT) to = llm;
  if (agentMessageDto.to == ToolRoleType.TOOL) to = tool;
  if (agentMessageDto.to == ToolRoleType.CLIENT) to = client;
  if (agentMessageDto.to == ToolRoleType.REFLECTION) to = reflection;

  if (agentMessageDto.reasoningContent != null) {
    print("#oriSid:${sessionId};parentTaskId:${agentMessageDto.parentTaskId};sid:${agentMessageDto.sessionId};taskId:${agentMessageDto.taskId}# $role -> $to: [reasoning]: ${agentMessageDto.reasoningContent}");
  }
  if (role.isNotEmpty && to.isNotEmpty) {
    print("#oriSid:${sessionId};parentTaskId:${agentMessageDto.parentTaskId};sid:${agentMessageDto.sessionId};taskId:${agentMessageDto.taskId}# $role -> $to: [${agentMessageDto.type}] ${message.replaceAll("\n", "\\n")}");
  }
}

bool hasFirst = false;
void listenChunk(String sessionId, AgentMessageChunkDto agentMessageChunkDto) {
  String user = "🧩👤USER  ";
  String agent = "🧩🤖AGENT ";
  String client = "🧩🔗CLIENT";

  String part = "";
  if (agentMessageChunkDto.type == TextMessageType.TEXT || agentMessageChunkDto.type == ReasoningMessageType.REASONING)
    part = agentMessageChunkDto.part as String;
  if (agentMessageChunkDto.type == TextMessageType.TASK_STATUS || agentMessageChunkDto.type == ReasoningMessageType.TASK_STATUS) {
    part = jsonEncode(agentMessageChunkDto.part as TaskStatusDto);
  }

  String  role = "";
  if (agentMessageChunkDto.role == ToolRoleType.USER)  role = user;
  if (agentMessageChunkDto.role == ToolRoleType.AGENT)  role = agent;
  if (agentMessageChunkDto.role == ToolRoleType.CLIENT)  role = client;

  String to = "";
  if (agentMessageChunkDto.to == ToolRoleType.USER) to = user;
  if (agentMessageChunkDto.to == ToolRoleType.AGENT) to = agent;
  if (agentMessageChunkDto.to == ToolRoleType.CLIENT)  to = client;

  if(hasFirst == false && agentMessageChunkDto.type == TextMessageType.TEXT) {
    stdout.write(("#oriSid:${sessionId};sid:${agentMessageChunkDto.sessionId};taskId:${agentMessageChunkDto.taskId}# $role -> $to: [${agentMessageChunkDto.type}] $part"));
    hasFirst = true;
  } else if(agentMessageChunkDto.type == TextMessageType.TASK_STATUS) {
    stdout.write("\n");
    print(("#oriSid:${sessionId};sid:${agentMessageChunkDto.sessionId};taskId:${agentMessageChunkDto.taskId}# $role -> $to: [${agentMessageChunkDto.type}] $part"));
    hasFirst = false;
  } else {
    stdout.write(agentMessageChunkDto.part);
  }
}