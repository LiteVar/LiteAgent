import 'package:event_bus/event_bus.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/models/local/tool.dart';

EventBus eventBus = EventBus();

class EventBusMessage {
  static const String updateList = "updateList";
  static const String updateSingleData = "updateSingleData";
  static const String startChat = "startChat";
  static const String login = "login";
  static const String logout = "logout";
  static const String sync = "synchronization";
  static const String delete = "delete";
  static const String switchPage = "switchPage";
}

class MessageEvent {
  String message = "";
  dynamic data;

  MessageEvent({required this.message, this.data});
}

class ToolMessageEvent {
  String message = "";
  ToolModel? tool;

  ToolMessageEvent({required this.message, this.tool});
}

class ModelMessageEvent {
  String message = "";
  ModelData? model;

  ModelMessageEvent({required this.message, this.model});
}

class AgentMessageEvent {
  String message = "";
  AgentModel? agent;

  AgentMessageEvent({required this.message, this.agent});
}
