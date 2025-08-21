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
      ..roleName = fields[1] as String
      ..message = fields[2] as String
      ..imgFilePath = fields[3] as String
      ..childAgentMessageList = (fields[4] as List?)?.cast<String>()
      ..toolName = fields[5] as String?
      ..thoughtList = (fields[6] as List?)?.cast<Thought>()
      ..taskId = fields[7] as String?
      ..subMessages = (fields[8] as List?)?.cast<ChatMessage>()
      ..receivedMessage = fields[9] as String?;
  }

  @override
  void write(BinaryWriter writer, ChatMessage obj) {
    writer
      ..writeByte(10)
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
      ..writeByte(6)
      ..write(obj.thoughtList)
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
      other is ChatMessageAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

class ThoughtAdapter extends TypeAdapter<Thought> {
  @override
  final int typeId = 6;

  @override
  Thought read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return Thought()
      ..id = fields[0] as String
      ..type = fields[1] as String
      ..roleName = fields[2] as String
      ..sentMessage = fields[3] as String
      ..receivedMessage = fields[4] as String;
  }

  @override
  void write(BinaryWriter writer, Thought obj) {
    writer
      ..writeByte(5)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.type)
      ..writeByte(2)
      ..write(obj.roleName)
      ..writeByte(3)
      ..write(obj.sentMessage)
      ..writeByte(4)
      ..write(obj.receivedMessage);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ThoughtAdapter &&
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
      ..key = fields[3] as String
      ..maxToken = fields[4] as String?
      ..createTime = fields[5] as int?
      ..type = fields[6] as String?
      ..nickName = fields[7] as String?
      ..supportMultiAgent = fields[8] as bool?
      ..supportToolCalling = fields[9] as bool?
      ..supportDeepThinking = fields[10] as bool?;
  }

  @override
  void write(BinaryWriter writer, ModelBean obj) {
    writer
      ..writeByte(11)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.name)
      ..writeByte(2)
      ..write(obj.url)
      ..writeByte(3)
      ..write(obj.key)
      ..writeByte(4)
      ..write(obj.maxToken)
      ..writeByte(5)
      ..write(obj.createTime)
      ..writeByte(6)
      ..write(obj.type)
      ..writeByte(7)
      ..write(obj.nickName)
      ..writeByte(8)
      ..write(obj.supportMultiAgent)
      ..writeByte(9)
      ..write(obj.supportToolCalling)
      ..writeByte(10)
      ..write(obj.supportDeepThinking);
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
      ..apiText = fields[6] as String
      ..createTime = fields[7] as int?
      ..thirdSchemaText = fields[8] as String?
      ..mcpText = fields[9] as String?
      ..supportMultiAgent = fields[10] as bool?;
  }

  @override
  void write(BinaryWriter writer, ToolBean obj) {
    writer
      ..writeByte(11)
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
      ..write(obj.apiText)
      ..writeByte(7)
      ..write(obj.createTime)
      ..writeByte(8)
      ..write(obj.thirdSchemaText)
      ..writeByte(9)
      ..write(obj.mcpText)
      ..writeByte(10)
      ..write(obj.supportMultiAgent);
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
      toolList: (fields[5] as List?)?.cast<String>(),
      prompt: fields[6] as String,
      temperature: fields[7] as double,
      maxToken: fields[8] as int,
      topP: fields[9] as double,
    )
      ..isCloud = fields[10] as bool?
      ..createTime = fields[11] as int?
      ..functionList = (fields[12] as List?)?.cast<AgentToolFunction>()
      ..libraryIds = (fields[13] as List?)?.cast<String>()
      ..operationMode = fields[14] as int?
      ..agentType = fields[15] as int?
      ..childAgentIds = (fields[16] as List?)?.cast<String>()
      ..toolOperationMode = fields[17] as int?
      ..ttsModelId = fields[18] as String?
      ..asrModelId = fields[19] as String?
      ..autoAgentFlag = fields[20] as bool?;
  }

  @override
  void write(BinaryWriter writer, AgentBean obj) {
    writer
      ..writeByte(21)
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
      ..write(obj.isCloud)
      ..writeByte(11)
      ..write(obj.createTime)
      ..writeByte(12)
      ..write(obj.functionList)
      ..writeByte(13)
      ..write(obj.libraryIds)
      ..writeByte(14)
      ..write(obj.operationMode)
      ..writeByte(15)
      ..write(obj.agentType)
      ..writeByte(16)
      ..write(obj.childAgentIds)
      ..writeByte(17)
      ..write(obj.toolOperationMode)
      ..writeByte(18)
      ..write(obj.ttsModelId)
      ..writeByte(19)
      ..write(obj.asrModelId)
      ..writeByte(20)
      ..write(obj.autoAgentFlag);
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

class AgentToolFunctionAdapter extends TypeAdapter<AgentToolFunction> {
  @override
  final int typeId = 5;

  @override
  AgentToolFunction read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return AgentToolFunction()
      ..toolId = fields[0] as String
      ..toolName = fields[1] as String
      ..functionName = fields[2] as String
      ..functionDescription = fields[3] as String
      ..operationMode = fields[4] as int
      ..requestMethod = fields[5] as String?
      ..isThirdTool = fields[6] as bool?;
  }

  @override
  void write(BinaryWriter writer, AgentToolFunction obj) {
    writer
      ..writeByte(7)
      ..writeByte(0)
      ..write(obj.toolId)
      ..writeByte(1)
      ..write(obj.toolName)
      ..writeByte(2)
      ..write(obj.functionName)
      ..writeByte(3)
      ..write(obj.functionDescription)
      ..writeByte(4)
      ..write(obj.operationMode)
      ..writeByte(5)
      ..write(obj.requestMethod)
      ..writeByte(6)
      ..write(obj.isThirdTool);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AgentToolFunctionAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
