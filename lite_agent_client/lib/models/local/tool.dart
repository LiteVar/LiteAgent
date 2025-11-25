import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../config/constants.dart';

import 'function.dart';

part 'tool.g.dart';

@JsonSerializable()
@HiveType(typeId: HiveTypeIds.toolModelTypeId)
class ToolModel extends Object{
  @HiveField(0)
  String id = "";
  @HiveField(1)
  String name = "";
  @HiveField(2)
  String description = "";
  @HiveField(3)
  String schemaType = "";
  @HiveField(4)
  String schemaText = "";
  @HiveField(5)
  String apiType = "";
  @HiveField(6)
  String apiText = "";
  @HiveField(7)
  int? createTime = 0;
  @HiveField(8)
  @Deprecated("deprecated in 1.0.0,use schemaText according to schemaType")
  String? thirdSchemaText = "";
  @HiveField(9)
  @Deprecated("deprecated in 1.0.0,use schemaText according to schemaType")
  String? mcpText = "";
  @HiveField(10)
  bool? supportMultiAgent;

  List<ToolFunctionModel> functionList = <ToolFunctionModel>[];
  bool shareFlag = false;
  bool isCloud = false;

  ToolModel({
    required this.id,
    required this.name,
    this.description = "",
    required this.schemaType,
    required this.schemaText,
    this.apiType = "",
    this.apiText = "",
    this.createTime = 0,
    this.supportMultiAgent = false,
    this.shareFlag = false,
    this.isCloud = false,
  });

  factory ToolModel.newEmptyTool({required String id, required int createTime}) {
    return ToolModel(id: id, name: '', description: '', schemaType: '', schemaText: '', createTime: createTime, supportMultiAgent: false);
  }


  factory ToolModel.fromJson(Map<String, dynamic> srcJson) => _$ToolModelFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ToolModelToJson(this);
}
