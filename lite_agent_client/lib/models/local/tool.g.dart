// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tool.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ToolModelAdapter extends TypeAdapter<ToolModel> {
  @override
  final int typeId = 1;

  @override
  ToolModel read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ToolModel(
      id: fields[0] as String,
      name: fields[1] as String,
      description: fields[2] as String,
      schemaType: fields[3] as String,
      schemaText: fields[4] as String,
      apiType: fields[5] as String,
      apiText: fields[6] as String,
      createTime: fields[7] as int?,
      supportMultiAgent: fields[10] as bool?,
    )
      ..thirdSchemaText = fields[8] as String?
      ..mcpText = fields[9] as String?;
  }

  @override
  void write(BinaryWriter writer, ToolModel obj) {
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
      other is ToolModelAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ToolModel _$ToolModelFromJson(Map<String, dynamic> json) => ToolModel(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String? ?? "",
      schemaType: json['schemaType'] as String,
      schemaText: json['schemaText'] as String,
      apiType: json['apiType'] as String? ?? "",
      apiText: json['apiText'] as String? ?? "",
      createTime: (json['createTime'] as num?)?.toInt() ?? 0,
      supportMultiAgent: json['supportMultiAgent'] as bool? ?? false,
      shareFlag: json['shareFlag'] as bool? ?? false,
      isCloud: json['isCloud'] as bool? ?? false,
    )
      ..thirdSchemaText = json['thirdSchemaText'] as String?
      ..mcpText = json['mcpText'] as String?
      ..functionList = (json['functionList'] as List<dynamic>)
          .map((e) => ToolFunctionModel.fromJson(e as Map<String, dynamic>))
          .toList();

Map<String, dynamic> _$ToolModelToJson(ToolModel instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'schemaType': instance.schemaType,
      'schemaText': instance.schemaText,
      'apiType': instance.apiType,
      'apiText': instance.apiText,
      'createTime': instance.createTime,
      'thirdSchemaText': instance.thirdSchemaText,
      'mcpText': instance.mcpText,
      'supportMultiAgent': instance.supportMultiAgent,
      'functionList': instance.functionList,
      'shareFlag': instance.shareFlag,
      'isCloud': instance.isCloud,
    };
