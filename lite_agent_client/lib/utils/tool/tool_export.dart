import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';

import '../../models/dto/open_tool_schema.dart';

class ToolExportUtil {
  static Future<String?> exportToolToJson(ToolDTO toolDTO, {bool exportPlaintext = false}) async {
    try {
      final exportData = await buildToolExportData(toolDTO, exportPlaintext: exportPlaintext);
      final fileName = _generateFileName(toolDTO);

      final savePath =
          await FilePicker.platform.saveFile(dialogTitle: '选择保存位置', fileName: fileName, type: FileType.custom, allowedExtensions: ['json']);

      if (savePath != null) {
        await File(savePath).writeAsString(const JsonEncoder.withIndent('  ').convert(exportData), encoding: utf8);
        Log.i("工具数据导出成功: $savePath");
      }

      return savePath;
    } catch (e) {
      Log.e("导出工具数据失败", e);
      return null;
    }
  }

  /// 去掉MCP的schemaStr中的敏感字段
  static String removeMCPSensitiveFields(String schemaStr) {
    final json = jsonDecode(schemaStr) as Map<String, dynamic>;
    final mcpServers = json['mcpServers'];
    if (mcpServers is! Map<String, dynamic>) return schemaStr;

    for (final serverConfig in mcpServers.values) {
      if (serverConfig is Map<String, dynamic>) {
        final env = serverConfig['env'];
        if (env is Map<String, dynamic>) {
          for (final entry in env.entries) {
            if (entry.value is String) {
              env[entry.key] = '{{<${entry.key.toUpperCase()}>}}';
            }
          }
        }
      }
    }
    return jsonEncode(json);
  }

  /// 去掉OPENTOOL的schemaStr中的敏感字段
  static String removeOpenToolSensitiveFields(String schemaStr) {
    OpenToolSchemaDTO dto = OpenToolSchemaDTO.fromJson(jsonDecode(schemaStr));
    dto.apiKey = dto.apiKey.isNotEmpty ? "{{<APIKEY>}}" : "";
    dto.serverUrl = dto.serverUrl.isNotEmpty ? "{{<SERVER_URL>}}" : "";
    return jsonEncode(dto.toJson());
  }

  static Future<Map<String, dynamic>> buildToolExportData(ToolDTO toolDTO, {bool exportPlaintext = false}) async {
    String schemaStr = toolDTO.schemaStr;
    if (!exportPlaintext) {
      if (toolDTO.schemaType == ToolValidator.DTO_MCP && ToolValidator.isValidMCPServersJson(schemaStr)) {
        schemaStr = removeMCPSensitiveFields(schemaStr);
      }

      if (toolDTO.schemaType == ToolValidator.DTO_OPEN_TOOL_SERVER && await ToolValidator.isValidOpenToolServerJson(schemaStr)) {
        schemaStr = removeOpenToolSensitiveFields(schemaStr);
      }
    }

    return {
      "name": toolDTO.name,
      "schemaType": toolDTO.schemaType,
      "schemaStr": schemaStr,
      "description": toolDTO.description,
      "apiKeyType": toolDTO.apiKeyType.toString().trimmed().toLowerCase(),
      "apiKey": toolDTO.apiKey.isNotEmpty ? (exportPlaintext ? toolDTO.apiKey : "{{<APIKEY>}}") : "",
      "autoAgent": toolDTO.autoAgent,
    };
  }

  static String _generateFileName(ToolDTO toolDTO) {
    final name = toolDTO.name.isEmpty ? "tool_${toolDTO.id.lastSixChars}" : toolDTO.name;
    return "$name.json";
  }
}
