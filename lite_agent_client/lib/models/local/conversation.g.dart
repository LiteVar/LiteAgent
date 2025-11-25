// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'conversation.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ConversationModelAdapter extends TypeAdapter<ConversationModel> {
  @override
  final int typeId = 4;

  @override
  ConversationModel read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ConversationModel()
      ..agentId = fields[0] as String
      ..chatMessageList = (fields[1] as List).cast<ChatMessageModel>()
      ..isCloud = fields[2] as bool
      ..updateTime = fields[3] as int?;
  }

  @override
  void write(BinaryWriter writer, ConversationModel obj) {
    writer
      ..writeByte(4)
      ..writeByte(0)
      ..write(obj.agentId)
      ..writeByte(1)
      ..write(obj.chatMessageList)
      ..writeByte(2)
      ..write(obj.isCloud)
      ..writeByte(3)
      ..write(obj.updateTime);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ConversationModelAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ConversationModel _$ConversationModelFromJson(Map<String, dynamic> json) =>
    ConversationModel()
      ..agentId = json['agentId'] as String
      ..chatMessageList = (json['chatMessageList'] as List<dynamic>)
          .map((e) => ChatMessageModel.fromJson(e as Map<String, dynamic>))
          .toList()
      ..isCloud = json['isCloud'] as bool
      ..updateTime = (json['updateTime'] as num?)?.toInt()
      ..agent = json['agent'] == null
          ? null
          : AgentModel.fromJson(json['agent'] as Map<String, dynamic>);

Map<String, dynamic> _$ConversationModelToJson(ConversationModel instance) =>
    <String, dynamic>{
      'agentId': instance.agentId,
      'chatMessageList': instance.chatMessageList,
      'isCloud': instance.isCloud,
      'updateTime': instance.updateTime,
      'agent': instance.agent,
    };
