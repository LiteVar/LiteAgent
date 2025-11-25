// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'model.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ModelDataAdapter extends TypeAdapter<ModelData> {
  @override
  final int typeId = 2;

  @override
  ModelData read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ModelData(
      id: fields[0] as String,
      name: fields[1] as String,
      url: fields[2] as String,
      key: fields[3] as String,
      maxToken: fields[4] as String?,
      createTime: fields[5] as int?,
      type: fields[6] as String?,
      alias: fields[7] as String?,
      supportMultiAgent: fields[8] as bool?,
      supportToolCalling: fields[9] as bool?,
      supportDeepThinking: fields[10] as bool?,
    );
  }

  @override
  void write(BinaryWriter writer, ModelData obj) {
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
      ..write(obj.alias)
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
      other is ModelDataAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModelData _$ModelDataFromJson(Map<String, dynamic> json) => ModelData(
      id: json['id'] as String,
      name: json['name'] as String,
      url: json['url'] as String,
      key: json['key'] as String,
      maxToken: json['maxToken'] as String? ?? "4096",
      createTime: (json['createTime'] as num?)?.toInt() ?? 0,
      type: json['type'] as String?,
      alias: json['alias'] as String?,
      supportMultiAgent: json['supportMultiAgent'] as bool? ?? false,
      supportToolCalling: json['supportToolCalling'] as bool? ?? true,
      supportDeepThinking: json['supportDeepThinking'] as bool? ?? false,
    );

Map<String, dynamic> _$ModelDataToJson(ModelData instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'url': instance.url,
      'key': instance.key,
      'maxToken': instance.maxToken,
      'createTime': instance.createTime,
      'type': instance.type,
      'alias': instance.alias,
      'supportMultiAgent': instance.supportMultiAgent,
      'supportToolCalling': instance.supportToolCalling,
      'supportDeepThinking': instance.supportDeepThinking,
    };
