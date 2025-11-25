import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../config/constants.dart';
import '../dto/function.dart';

part 'function.g.dart';

@JsonSerializable()
@HiveType(typeId: HiveTypeIds.toolFunctionModelTypeId)
class ToolFunctionModel extends Object{
  @HiveField(0)
  var toolId = "";
  @HiveField(1)
  var toolName = "";
  @HiveField(2)
  var functionName = "";
  @HiveField(3)
  var functionDescription = "";
  @HiveField(4)
  @Deprecated("deprecated in 0.2.0,not support")
  int? operationMode = 0;
  @HiveField(5)
  String? requestMethod = "";
  @HiveField(6)
  bool? isThirdTool = false;

  // 绑定状态相关字段（不持久化）
  bool isBound = false;
  String? boundAgentId;
  String? boundAgentName;
  String? triggerMethod;

  FunctionDto translateToDTO() {
    return FunctionDto(toolId, functionName, requestMethod ?? "", 0, "");
  }

  void translateFromDTO(FunctionDto dto) {
    toolId = dto.toolId ?? "";
    toolName = "";
    functionName = dto.functionName;
    functionDescription = "";
    operationMode = 0;
    requestMethod = dto.requestMethod;
    isThirdTool = dto.protocol == "external";
  }

  ToolFunctionModel();

  factory ToolFunctionModel.fromJson(Map<String, dynamic> srcJson) => _$ToolFunctionModelFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ToolFunctionModelToJson(this);
}
