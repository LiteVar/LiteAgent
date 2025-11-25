// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'thought.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class ThoughtModelAdapter extends TypeAdapter<ThoughtModel> {
  @override
  final int typeId = 6;

  @override
  ThoughtModel read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return ThoughtModel()
      ..id = fields[0] as String
      ..type = fields[1] as String
      ..roleName = fields[2] as String
      ..sentMessage = fields[3] as String
      ..receivedMessage = fields[4] as String;
  }

  @override
  void write(BinaryWriter writer, ThoughtModel obj) {
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
      other is ThoughtModelAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ThoughtModel _$ThoughtModelFromJson(Map<String, dynamic> json) => ThoughtModel()
  ..id = json['id'] as String
  ..type = json['type'] as String
  ..roleName = json['roleName'] as String
  ..sentMessage = json['sentMessage'] as String
  ..receivedMessage = json['receivedMessage'] as String;

Map<String, dynamic> _$ThoughtModelToJson(ThoughtModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'type': instance.type,
      'roleName': instance.roleName,
      'sentMessage': instance.sentMessage,
      'receivedMessage': instance.receivedMessage,
    };
