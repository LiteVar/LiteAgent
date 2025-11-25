// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'agent.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class AgentModelAdapter extends TypeAdapter<AgentModel> {
  @override
  final int typeId = 0;

  @override
  AgentModel read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return AgentModel(
      id: fields[0] as String,
      name: fields[1] as String,
      iconPath: fields[2] as String,
      description: fields[3] as String,
      modelId: fields[4] as String,
      prompt: fields[6] as String,
      temperature: fields[7] as double,
      maxToken: fields[8] as int,
      topP: fields[9] as double,
      isCloud: fields[10] as bool?,
      createTime: fields[11] as int?,
      functionList: (fields[12] as List?)?.cast<ToolFunctionModel>(),
      libraryIds: (fields[13] as List?)?.cast<String>(),
      operationMode: fields[14] as int?,
      agentType: fields[15] as int?,
      childAgentIds: (fields[16] as List?)?.cast<String>(),
      toolOperationMode: fields[17] as int?,
      ttsModelId: fields[18] as String?,
      asrModelId: fields[19] as String?,
      autoAgentFlag: fields[20] as bool?,
    )..toolList = (fields[5] as List?)?.cast<String>();
  }

  @override
  void write(BinaryWriter writer, AgentModel obj) {
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
      other is AgentModelAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AgentModel _$AgentModelFromJson(Map<String, dynamic> json) => AgentModel(
      id: json['id'] as String,
      name: json['name'] as String,
      iconPath: json['iconPath'] as String,
      description: json['description'] as String,
      modelId: json['modelId'] as String? ?? "",
      prompt: json['prompt'] as String? ?? "",
      temperature: (json['temperature'] as num?)?.toDouble() ?? 0.0,
      maxToken: (json['maxToken'] as num?)?.toInt() ?? 4096,
      topP: (json['topP'] as num?)?.toDouble() ?? 1.0,
      isCloud: json['isCloud'] as bool? ?? false,
      createTime: (json['createTime'] as num?)?.toInt() ?? 0,
      functionList: (json['functionList'] as List<dynamic>?)
          ?.map((e) => ToolFunctionModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      libraryIds: (json['libraryIds'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      operationMode: (json['operationMode'] as num?)?.toInt() ?? 0,
      agentType: (json['agentType'] as num?)?.toInt() ?? 0,
      childAgentIds: (json['childAgentIds'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      toolOperationMode: (json['toolOperationMode'] as num?)?.toInt() ?? 0,
      ttsModelId: json['ttsModelId'] as String?,
      asrModelId: json['asrModelId'] as String?,
      autoAgentFlag: json['autoAgentFlag'] as bool? ?? false,
      shareFlag: json['shareFlag'] as bool? ?? false,
    )..toolList =
        (json['toolList'] as List<dynamic>?)?.map((e) => e as String).toList();

Map<String, dynamic> _$AgentModelToJson(AgentModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'iconPath': instance.iconPath,
      'description': instance.description,
      'modelId': instance.modelId,
      'toolList': instance.toolList,
      'prompt': instance.prompt,
      'temperature': instance.temperature,
      'maxToken': instance.maxToken,
      'topP': instance.topP,
      'isCloud': instance.isCloud,
      'createTime': instance.createTime,
      'functionList': instance.functionList,
      'libraryIds': instance.libraryIds,
      'operationMode': instance.operationMode,
      'agentType': instance.agentType,
      'childAgentIds': instance.childAgentIds,
      'toolOperationMode': instance.toolOperationMode,
      'ttsModelId': instance.ttsModelId,
      'asrModelId': instance.asrModelId,
      'autoAgentFlag': instance.autoAgentFlag,
      'shareFlag': instance.shareFlag,
    };
