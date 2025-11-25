import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../config/constants.dart';
import 'agent.dart';
import 'message.dart';

part 'conversation.g.dart';

@JsonSerializable()
@HiveType(typeId: HiveTypeIds.conversationModelTypeId)
class ConversationModel extends Object{
  @HiveField(0)
  String agentId = "";
  @HiveField(1)
  var chatMessageList = <ChatMessageModel>[];
  @HiveField(2)
  bool isCloud = false;
  @HiveField(3)
  int? updateTime = 0;

  AgentModel? agent;

  ConversationModel();


  factory ConversationModel.fromJson(Map<String, dynamic> srcJson) => _$ConversationModelFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ConversationModelToJson(this);
}