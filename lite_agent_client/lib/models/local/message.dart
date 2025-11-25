import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../config/constants.dart';

part 'message.g.dart';

@JsonSerializable()
@HiveType(typeId: HiveTypeIds.messageModelTypeId)
class ChatMessageModel extends Object {
  @HiveField(0)
  int sendRole = 0;
  @HiveField(1)
  String roleName = "";
  @HiveField(2)
  String message = "";
  @HiveField(3)
  String imgFilePath = "";
  @HiveField(4)
  @Deprecated("deprecated in 0.2.0,use subMessages")
  List<String>? childAgentMessageList;
  @HiveField(5)
  @Deprecated("deprecated in 0.2.0,use roleName")
  String? toolName;

  //@HiveField(6)
  //@Deprecated("deprecated in 1.0.0,use subMessages")
  //List<ThoughtModel>? thoughtList;
  @HiveField(7)
  String? taskId = ""; //sometime for MessagesTaskId,sometime for functionId,according to ChatRoleType
  @HiveField(8)
  List<ChatMessageModel>? subMessages;
  @HiveField(9)
  String? receivedMessage = ""; //only for subMessages

  bool isLoading = false;
  bool isMessageExpanded = true;

  ChatMessageModel();

  factory ChatMessageModel.fromJson(Map<String, dynamic> srcJson) => _$ChatMessageModelFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ChatMessageModelToJson(this);
}

class ChatRoleType {
  static const int User = 0;
  static const int Agent = 1;
  static const int SubAgent = 2;
  static const int Tool = 3;
  static const int Reflection = 4;
  static const int Reasoning = 5;
}
