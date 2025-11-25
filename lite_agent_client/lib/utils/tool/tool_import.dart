import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/batch_id_generator.dart';
import 'package:lite_agent_client/utils/extension/tool_extension.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';
import 'package:lite_agent_client/config/constants.dart';

class ToolImportUtil {
  /// 为Agent导入单个工具，返回带该ID的ToolDTO
  static Future<ToolDTO> importSingleToolForAgent(ToolDTO toolToImport, String newId, int? operate) async {
    String? similarId = toolToImport.similarId ?? "";
    // 跳过操作：直接使用现有工具，不创建新工具
    if (operate == ImportOperate.operateSkip) {
      final existingTool = await toolRepository.getData(similarId);
      if (existingTool == null) {
        // 如果相似工具不存在，降级为覆盖操作
        Log.i("相似工具 $similarId 不存在，降级为覆盖操作");
        return importSingleToolForAgent(toolToImport, newId, ImportOperate.operateOverwrite);
      }
      return existingTool.toDTO();
    }

    final String targetId;
    final similarTool = await toolRepository.getData(similarId);
    if (similarId.isEmpty || operate == null || operate == ImportOperate.operateNew) {
      targetId = newId;
      // while (!await ToolValidator.isNameUniqueAsync(toolToImport.name)) {
      //   toolToImport.name += "_1";
      // }
    } else {
      targetId = similarId;
    }
    final toolModel = _createImportToolData(toolToImport, targetId);
    if (operate == ImportOperate.operateOverwrite && similarTool?.createTime != null) {
      toolModel.createTime = similarTool?.createTime;
    }
    await toolRepository.updateTool(toolModel.id, toolModel);

    Log.i("导入单个工具成功(operate: $operate): ${toolModel.name} (${toolModel.id})");
    return toolModel.toDTO();
  }

  /// 验证单个工具JSON文件的有效性
  static Future<({ToolDTO? toolDTO, String? error})> validateSingleFile(File file) async {
    try {
      final content = await file.readAsString();
      final fileName = file.path.split(Platform.pathSeparator).last;

      if (!content.isJson()) {
        return (toolDTO: null, error: "文件 $fileName 不是有效的JSON格式");
      }

      final toolDTO = await _parseToolFromJson(content, fileName);
      if (toolDTO != null) {
        return (toolDTO: toolDTO, error: null);
      } else {
        return (toolDTO: null, error: "文件 $fileName 格式不正确或 schema 格式校验失败");
      }
    } catch (e) {
      final safeName = file.path.split(Platform.pathSeparator).last;
      return (toolDTO: null, error: "读取文件 $safeName 失败: $e");
    }
  }

  /// 从JSON内容解析工具DTO，验证其有效性
  static Future<ToolDTO?> _parseToolFromJson(String jsonContent, String fileName) async {
    try {
      final jsonData = json.decode(jsonContent) as Map<String, dynamic>;

      final isValid = await ToolValidator.validateToolDTOJsonConfig(jsonData);
      if (!isValid) {
        Log.w("文件 $fileName 中的工具配置无效");
        return null;
      }

      return ToolDTO.fromJson(jsonData);
    } catch (e) {
      return null;
    }
  }

  /// 创建导入用的ToolModel
  static ToolModel _createImportToolData(ToolDTO toolToImport, String newId) {
    final toolModel = toolToImport.toModel();
    toolModel.id = newId;
    toolModel.createTime = DateTime.now().microsecondsSinceEpoch;
    return toolModel;
  }

  /// 验证工具名称唯一性，返回名称错误
  static Future<List<String>> validateToolNames(List<ToolDTO> tools) async {
    final nameErrors = <String>[];
    if (tools.isEmpty) return nameErrors;

    final existingTools = (await toolRepository.getToolListFromBox()).toList();
    final importNames = <String>[];

    for (final tool in tools) {
      final name = tool.name;

      // 检查与现有工具的名称冲突
      if (!ToolValidator.isNameUnique(name, existingTools)) {
        nameErrors.add("工具 '$name' 已存在于现有工具中");
      } else if (importNames.contains(name)) {
        nameErrors.add("导入的工具中有多个工具使用了相同的名称 '$name'");
      } else {
        importNames.add(name);
      }
    }

    return nameErrors;
  }
}
