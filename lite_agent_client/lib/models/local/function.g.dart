// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'function.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ToolFunctionModelAdapter extends TypeAdapter<ToolFunctionModel> {
  @override
  final int typeId = 5;

  @override
  ToolFunctionModel read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ToolFunctionModel()
      ..toolId = fields[0] as String
      ..toolName = fields[1] as String
      ..functionName = fields[2] as String
      ..functionDescription = fields[3] as String
      ..operationMode = fields[4] as int?
      ..requestMethod = fields[5] as String?
      ..isThirdTool = fields[6] as bool?;
  }

  @override
  void write(BinaryWriter writer, ToolFunctionModel obj) {
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
      other is ToolFunctionModelAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ToolFunctionModel _$ToolFunctionModelFromJson(Map<String, dynamic> json) =>
    ToolFunctionModel()
      ..toolId = json['toolId'] as String
      ..toolName = json['toolName'] as String
      ..functionName = json['functionName'] as String
      ..functionDescription = json['functionDescription'] as String
      ..operationMode = (json['operationMode'] as num?)?.toInt()
      ..requestMethod = json['requestMethod'] as String?
      ..isThirdTool = json['isThirdTool'] as bool?
      ..isBound = json['isBound'] as bool
      ..boundAgentId = json['boundAgentId'] as String?
      ..boundAgentName = json['boundAgentName'] as String?
      ..triggerMethod = json['triggerMethod'] as String?;

Map<String, dynamic> _$ToolFunctionModelToJson(ToolFunctionModel instance) =>
    <String, dynamic>{
      'toolId': instance.toolId,
      'toolName': instance.toolName,
      'functionName': instance.functionName,
      'functionDescription': instance.functionDescription,
      'operationMode': instance.operationMode,
      'requestMethod': instance.requestMethod,
      'isThirdTool': instance.isThirdTool,
      'isBound': instance.isBound,
      'boundAgentId': instance.boundAgentId,
      'boundAgentName': instance.boundAgentName,
      'triggerMethod': instance.triggerMethod,
    };
