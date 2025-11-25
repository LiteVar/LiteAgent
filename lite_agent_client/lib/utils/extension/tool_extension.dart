import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/utils/tool/tool_converter.dart';
import 'package:lite_agent_client/utils/tool/tool_export.dart';
import 'package:lite_agent_client/utils/tool/tool_function_parser.dart';

import '../tool/tool_validator.dart';

/// 面向领域对象的轻量扩展：
/// - 聚合常用的转换与导出入口
/// - 不承载具体业务逻辑，实现仍委托给对应 util

extension ToolDTOExtensions on ToolDTO {
  /// DTO -> Model 转换（委托 ToolConverter）
  ToolModel toModel() => ToolConverter.dtoToModel(this);

  /// 直接导出为 JSON（委托 ToolExportUtil）
  Future<String?> exportJson(bool exportPlaintext) => ToolExportUtil.exportToolToJson(this, exportPlaintext: exportPlaintext);
}

extension ToolModelExtensions on ToolModel {
  /// Model -> DTO 转换（委托 ToolConverter）
  ToolDTO toDTO() => ToolConverter.modelToDto(this);

  /// 初始化/刷新函数列表（委托 ToolParser）
  Future<void> initFunctions() async {
    functionList = await ToolParser.parseFunctions(this);
  }
}
