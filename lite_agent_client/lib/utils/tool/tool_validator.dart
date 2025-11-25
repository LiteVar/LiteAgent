import 'dart:convert';

import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/tool/tool_converter.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:openapi_dart/openapi_dart.dart';
import 'package:openrpc_dart/openrpc_dart.dart';
import 'package:opentool_dart/opentool_dart.dart';
import 'package:yaml/yaml.dart';

import '../../models/dto/open_tool_schema.dart';
import '../../repositories/tool_repository.dart';
import '../log_util.dart';

/// 工具验证器 - 负责验证工具配置的有效性
class ToolValidator {
  /// 后端接口的schemaType规范
  static const int DTO_OPENAPI = 1;
  static const int DTO_JSON_RPC_HTTP = 2;
  static const int DTO_OPEN_TOOL = 4;
  static const int DTO_MCP = 5;
  static const int DTO_OPEN_TOOL_SERVER = 6;

  static const String OPTION_OPENAPI = "OpenAPI3(YAML/JSON)";
  static const String OPTION_JSONRPCHTTP = "OpenRPC(JSON)";
  static const String OPTION_OPENTOOL = "第三方OpenTool";
  static const String OPTION_MCP_STDIO_TOOLS = "MCP(stdio)";
  static const String OPTION_OPENTOOL_SERVER = "OpenTool";

  /// 获取所有支持的 schemaType 值
  static const List<int> supportedDTOTypes = [DTO_OPENAPI, DTO_JSON_RPC_HTTP, DTO_OPEN_TOOL, DTO_MCP, DTO_OPEN_TOOL_SERVER];
  static const List<String> supportedLocalTypes = [
    Protocol.OPENAPI,
    Protocol.JSONRPCHTTP,
    Protocol.OPENTOOL,
    Protocol.MCP_STDIO,
    OPTION_OPENTOOL_SERVER,
  ];

  /// 验证完整的工具配置
  static Future<bool> validateToolDTOJsonConfig(Map<String, dynamic> jsonData) async {
    // 验证必需字段
    if (!validateRequiredFields(jsonData)) {
      Log.w("工具配置缺少必需字段");
      return false;
    }

    // 验证 schemaType
    if (!validateSchemaType(jsonData['schemaType'])) {
      Log.w("不支持的 schemaType: ${jsonData['schemaType']}");
      return false;
    }

    // 验证 schema 格式
    final schemaType = jsonData['schemaType'] as int;
    final schemaStr = jsonData['schemaStr'].toString().trimmed();
    return await validateSchemaFormatFormDTO(schemaType, schemaStr);
  }

  /// 验证是否为有效的 OpenAPI JSON 格式
  static Future<bool> isValidOpenAIJson(String schemaStr) async {
    try {
      await OpenAPILoader().load(schemaStr);
      return true;
    } catch (e) {
      return false;
    }
  }

  /// 验证工具配置的必需字段
  static bool validateRequiredFields(Map<String, dynamic> jsonData) {
    // 检查 name 字段
    final name = jsonData['name'];
    if (name == null || name is! String || name.trim().isEmpty) {
      return false;
    }

    // 检查 schemaStr 字段
    final schemaStr = jsonData['schemaStr'];
    if (schemaStr == null || schemaStr is! String || schemaStr.trim().isEmpty) {
      return false;
    }

    // 检查 schemaType 字段
    final schemaType = jsonData['schemaType'];
    if (schemaType == null || schemaType is! int) {
      return false;
    }

    return true;
  }

  /// 验证 schemaType 是否有效
  static bool validateSchemaType(dynamic schemaType) {
    if (schemaType is int) {
      return isDTOSchemaTypeSupported(schemaType);
    } else if (schemaType is String) {
      return isSchemaTypeSupported(schemaType);
    }
    return false;
  }

  /// 检查 dtoSchemaType 是否受支持
  static bool isDTOSchemaTypeSupported(int schemaType) {
    return supportedDTOTypes.contains(schemaType);
  }

  /// 检查 localSchemaType 是否受支持
  static bool isSchemaTypeSupported(String schemaType) {
    return supportedLocalTypes.contains(schemaType);
  }

  /// 根据 schemaType 验证 schema 格式
  static Future<bool> validateSchemaFormatFormDTO(int schemaType, String schemaStr) async {
    String protocolType = ToolConverter.getModelProtocol(schemaType);
    return validateSchemaFormat(protocolType, schemaStr);
  }

  /// 根据 schemaType 验证 schema 格式
  static Future<bool> validateSchemaFormat(String schemaType, String schemaStr) async {
    switch (schemaType) {
      case Protocol.OPENAPI:
        return (await isValidOpenAIJson(schemaStr)) || await isValidOpenAIYaml(schemaStr);
      case Protocol.JSONRPCHTTP:
        return await isValidOpenPPCJson(schemaStr);
      case Protocol.OPENTOOL:
        return await isValidOpenToolJson(schemaStr);
      case Protocol.MCP_STDIO:
        return isValidMCPServersJson(schemaStr);
      case ToolValidator.OPTION_OPENTOOL_SERVER:
        return isValidOpenToolServerJson(schemaStr);
      default:
        return false;
    }
  }

  /// 验证是否为有效的 OpenAPI YAML 格式
  static Future<bool> isValidOpenAIYaml(String schemaStr) async {
    try {
      YamlMap yamlMap = loadYaml(schemaStr);
      await OpenAPILoader().load(jsonEncode(yamlMap));
      return true;
    } catch (e) {
      return false;
    }
  }

  /// 验证是否为有效的 JSON-RPC HTTP 格式
  static Future<bool> isValidOpenPPCJson(String schemaStr) async {
    try {
      await OpenRPCLoader().load(schemaStr);
      return true;
    } catch (e) {
      return false;
    }
  }

  /// 验证是否为有效的 OpenTool JSON 格式
  static Future<bool> isValidOpenToolJson(String schemaStr) async {
    try {
      await OpenToolJsonLoader().load(schemaStr);
      return true;
    } catch (e) {
      return false;
    }
  }

  /// 验证是否为有效的 MCP Servers JSON 格式
  static bool isValidMCPServersJson(String schemaStr) {
    try {
      Map<String, dynamic> json = jsonDecode(schemaStr);
      if (json["mcpServers"] == null) return false;
      json["mcpServers"].forEach((key, value) {
        if (value["command"] == null || value["args"] == null || value["command"] is! String || value["args"] is! List) {
          return false;
        }
      });
      return true;
    } catch (e) {
      return false;
    }
  }

  /// 验证是否为有效的 OpenTool JSON 格式
  static Future<bool> isValidOpenToolServerJson(String schemaStr) async {
    try {
      OpenToolSchemaDTO dto = OpenToolSchemaDTO.fromJson(jsonDecode(schemaStr));
      
      // 尝试修复 schema 中可能的类型问题（字符串数字转整数）
      String fixedSchema = _fixOpenToolSchemaTypes(dto.schema);
      
      await OpenToolJsonLoader().load(fixedSchema);
      return true;
    } catch (e) {
      Log.w("OpenTool Server JSON 验证失败: $e");
      return false;
    }
  }

  /// 修复 OpenTool schema 中可能的类型问题
  /// 将字符串形式的数字转换为整数（特别是枚举值）
  static String _fixOpenToolSchemaTypes(String schemaStr) {
    try {
      final schemaMap = jsonDecode(schemaStr) as Map<String, dynamic>;
      _fixNumericTypes(schemaMap);
      return jsonEncode(schemaMap);
    } catch (e) {
      // 如果修复失败，返回原始 schema
      Log.w("修复 OpenTool schema 类型失败: $e");
      return schemaStr;
    }
  }

  /// 递归修复 JSON 中的数值类型
  /// 特别处理 schema.enum 数组中的字符串数字
  static void _fixNumericTypes(dynamic data) {
    if (data is Map<String, dynamic>) {
      for (var key in data.keys) {
        final value = data[key];
        
        // 特别处理 enum 数组（OpenTool schema 中的枚举值）
        if (key == 'enum' && value is List) {
          for (var i = 0; i < value.length; i++) {
            if (value[i] is String) {
              final intValue = int.tryParse(value[i] as String);
              if (intValue != null) {
                value[i] = intValue;
              }
            }
          }
        } else if (value is String) {
          // 尝试将字符串数字转换为整数
          final intValue = int.tryParse(value);
          if (intValue != null && value == intValue.toString()) {
            data[key] = intValue;
          }
        } else if (value is Map || value is List) {
          _fixNumericTypes(value);
        }
      }
    } else if (data is List) {
      for (var i = 0; i < data.length; i++) {
        final value = data[i];
        if (value is String) {
          final intValue = int.tryParse(value);
          if (intValue != null && value == intValue.toString()) {
            data[i] = intValue;
          }
        } else if (value is Map || value is List) {
          _fixNumericTypes(value);
        }
      }
    }
  }

  /// 获取相同名字的工具ID
  static String? getSameNameToolId(String name, List<ToolModel> existingTools, {String? excludeId}) {
    if (name.trim().isEmpty) return null;

    for (final tool in existingTools) {
      // 如果指定了排除ID，则跳过该工具
      if (excludeId != null && tool.id == excludeId) continue;
      if (tool.name.trimmed() == name.trimmed()) {
        return tool.id;
      }
    }
    return null;
  }

  /// 检查工具名字是否唯一
  static bool isNameUnique(String name, List<ToolModel> existingTools, {String? excludeId}) {
    return getSameNameToolId(name, existingTools, excludeId: excludeId) == null;
  }

  /// 检查工具名字是否唯一（异步版本）
  static Future<bool> isNameUniqueAsync(String name, {String? excludeId}) async {
    if (name.trim().isEmpty) return false;
    final existingTools = (await toolRepository.getToolListFromBox()).toList();
    return getSameNameToolId(name, existingTools, excludeId: excludeId) == null;
  }
}
