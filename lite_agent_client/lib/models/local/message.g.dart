// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'message.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ChatMessageModelAdapter extends TypeAdapter<ChatMessageModel> {
  @override
  final int typeId = 3;

  @override
  ChatMessageModel read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ChatMessageModel()
      ..sendRole = fields[0] as int
      ..roleName = fields[1] as String
      ..message = fields[2] as String
      ..imgFilePath = fields[3] as String
      ..childAgentMessageList = (fields[4] as List?)?.cast<String>()
      ..toolName = fields[5] as String?
      ..taskId = fields[7] as String?
      ..subMessages = (fields[8] as List?)?.cast<ChatMessageModel>()
      ..receivedMessage = fields[9] as String?;
  }

  @override
  void write(BinaryWriter writer, ChatMessageModel obj) {
    writer
      ..writeByte(9)
      ..writeByte(0)
      ..write(obj.sendRole)
      ..writeByte(1)
      ..write(obj.roleName)
      ..writeByte(2)
      ..write(obj.message)
      ..writeByte(3)
      ..write(obj.imgFilePath)
      ..writeByte(4)
      ..write(obj.childAgentMessageList)
      ..writeByte(5)
      ..write(obj.toolName)
      ..writeByte(7)
      ..write(obj.taskId)
      ..writeByte(8)
      ..write(obj.subMessages)
      ..writeByte(9)
      ..write(obj.receivedMessage);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ChatMessageModelAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ChatMessageModel _$ChatMessageModelFromJson(Map<String, dynamic> json) =>
    ChatMessageModel()
      ..sendRole = (json['sendRole'] as num).toInt()
      ..roleName = json['roleName'] as String
      ..message = json['message'] as String
      ..imgFilePath = json['imgFilePath'] as String
      ..childAgentMessageList =
          (json['childAgentMessageList'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList()
      ..toolName = json['toolName'] as String?
      ..taskId = json['taskId'] as String?
      ..subMessages = (json['subMessages'] as List<dynamic>?)
          ?.map((e) => ChatMessageModel.fromJson(e as Map<String, dynamic>))
          .toList()
      ..receivedMessage = json['receivedMessage'] as String?
      ..isLoading = json['isLoading'] as bool
      ..isMessageExpanded = json['isMessageExpanded'] as bool;

Map<String, dynamic> _$ChatMessageModelToJson(ChatMessageModel instance) =>
    <String, dynamic>{
      'sendRole': instance.sendRole,
      'roleName': instance.roleName,
      'message': instance.message,
      'imgFilePath': instance.imgFilePath,
      'childAgentMessageList': instance.childAgentMessageList,
      'toolName': instance.toolName,
      'taskId': instance.taskId,
      'subMessages': instance.subMessages,
      'receivedMessage': instance.receivedMessage,
      'isLoading': instance.isLoading,
      'isMessageExpanded': instance.isMessageExpanded,
    };
