import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../config/constants.dart';

part 'thought.g.dart';

@JsonSerializable()
@Deprecated("deprecated in 1.0.0,use ChatMessage")
@HiveType(typeId: HiveTypeIds.thoughtModelTypeId)
class ThoughtModel extends Object {
  @HiveField(0)
  String id = "";
  @HiveField(1)
  String type = "";
  @HiveField(2)
  String roleName = "";
  @HiveField(3)
  String sentMessage = "";
  @HiveField(4)
  String receivedMessage = "";

  ThoughtModel();

  factory ThoughtModel.fromJson(Map<String, dynamic> srcJson) => _$ThoughtModelFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ThoughtModelToJson(this);
}

@Deprecated("deprecated in 1.0.0,use ChatRoleType")
class ThoughtRoleType {
  static const String Tool = "tool";
  static const String Agent = "agent";
}
