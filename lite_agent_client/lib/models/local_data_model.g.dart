// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'local_data_model.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ChatMessageAdapter extends TypeAdapter<ChatMessage> {
  @override
  final int typeId = 3;

  @override
  ChatMessage read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ChatMessage()
      ..sendRole = fields[0] as int
      ..userName = fields[1] as String
      ..message = fields[2] as String
      ..imgFilePath = fields[3] as String;
  }

  @override
  void write(BinaryWriter writer, ChatMessage obj) {
    writer
      ..writeByte(4)
      ..writeByte(0)
      ..write(obj.sendRole)
      ..writeByte(1)
      ..write(obj.userName)
      ..writeByte(2)
      ..write(obj.message)
      ..writeByte(3)
      ..write(obj.imgFilePath);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ChatMessageAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class AgentConversationBeanAdapter extends TypeAdapter<AgentConversationBean> {
  @override
  final int typeId = 4;

  @override
  AgentConversationBean read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return AgentConversationBean()
      ..agentId = fields[0] as String
      ..chatMessageList = (fields[1] as List).cast<ChatMessage>()
      ..isCloud = fields[2] as bool
      ..updateTime = fields[3] as int?;
  }

  @override
  void write(BinaryWriter writer, AgentConversationBean obj) {
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
      other is AgentConversationBeanAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class ModelBeanAdapter extends TypeAdapter<ModelBean> {
  @override
  final int typeId = 2;

  @override
  ModelBean read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ModelBean()
      ..id = fields[0] as String
      ..name = fields[1] as String
      ..url = fields[2] as String
      ..key = fields[3] as String;
  }

  @override
  void write(BinaryWriter writer, ModelBean obj) {
    writer
      ..writeByte(4)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.url)
      ..writeByte(3)
      ..write(obj.key);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ModelBeanAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class ToolBeanAdapter extends TypeAdapter<ToolBean> {
  @override
  final int typeId = 1;

  @override
  ToolBean read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ToolBean()
      ..id = fields[0] as String
      ..name = fields[1] as String
      ..description = fields[2] as String
      ..schemaType = fields[3] as String
      ..schemaText = fields[4] as String
      ..apiType = fields[5] as String
      ..apiText = fields[6] as String;
  }

  @override
  void write(BinaryWriter writer, ToolBean obj) {
    writer
      ..writeByte(7)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.description)
      ..writeByte(3)
      ..write(obj.schemaType)
      ..writeByte(4)
      ..write(obj.schemaText)
      ..writeByte(5)
      ..write(obj.apiType)
      ..writeByte(6)
      ..write(obj.apiText);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ToolBeanAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class AgentBeanAdapter extends TypeAdapter<AgentBean> {
  @override
  final int typeId = 0;

  @override
  AgentBean read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return AgentBean(
      id: fields[0] as String,
      name: fields[1] as String,
      iconPath: fields[2] as String,
      description: fields[3] as String,
      modelId: fields[4] as String,
      toolList: (fields[5] as List).cast<String>(),
      prompt: fields[6] as String,
      temperature: fields[7] as double,
      maxToken: fields[8] as int,
      topP: fields[9] as double,
    )..isCloud = fields[10] as bool?;
  }

  @override
  void write(BinaryWriter writer, AgentBean obj) {
    writer
      ..writeByte(11)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.iconPath)
      ..writeByte(3)
      ..write(obj.description)
      ..writeByte(4)
      ..write(obj.modelId)
      ..writeByte(5)
      ..write(obj.toolList)
      ..writeByte(6)
      ..write(obj.prompt)
      ..writeByte(7)
      ..write(obj.temperature)
      ..writeByte(8)
      ..write(obj.maxToken)
      ..writeByte(9)
      ..write(obj.topP)
      ..writeByte(10)
      ..write(obj.isCloud);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AgentBeanAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
