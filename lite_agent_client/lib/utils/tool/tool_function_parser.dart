import 'dart:convert';

import 'package:lite_agent_client/models/dto/function.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/server/local_server/mcp_service.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:yaml/yaml.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:opentool_dart/opentool_dart.dart';

/// 工具解析器 - 负责解析各种类型的工具 schema 并生成函数列表
class ToolParser {
  /// 解析工具函数列表
  static Future<List<ToolFunctionModel>> parseFunctions(ToolModel tool) async {
    if (tool.schemaText.isEmpty || tool.schemaType.isEmpty) {
      return [];
    }
    return await _parseFunctionsByType(tool);
  }

  /// 根据 schema 类型解析函数
  static Future<List<ToolFunctionModel>> _parseFunctionsByType(ToolModel tool) async {
    switch (tool.schemaType) {
      case Protocol.MCP_STDIO:
        return await _parseMCPFunctions(tool);
      case Protocol.OPENTOOL:
        return await _parseOpenToolFunctions(tool);
      case Protocol.JSONRPCHTTP:
        return await _parseJsonRPCFunctions(tool);
      case Protocol.OPENAPI:
        return await _parseOpenAPIFunctions(tool);
      case ToolValidator.OPTION_OPENTOOL_SERVER:
        return await _parseOpenToolServerFunctions(tool);
      default:
        return [];
    }
  }

  /// 解析 MCP 工具函数
  static Future<List<ToolFunctionModel>> _parseMCPFunctions(ToolModel tool) async {
    final functions = <ToolFunctionModel>[];
    final results = await McpService().checkServers(tool.schemaText);

    for (final result in results) {
      if (result.isAvailable && result.tools.isNotEmpty) {
        final serverName = result.serverId;
        for (final func in result.tools) {
          functions.add(ToolFunctionModel()
            ..toolId = tool.id
            ..toolName = tool.name
            ..functionName = "$serverName/${func.name}"
            ..functionDescription = func.description);
        }
      }
    }

    return functions;
  }

  /// 解析 OpenTool 工具函数
  static Future<List<ToolFunctionModel>> _parseOpenToolFunctions(ToolModel tool) async {
    final functions = <ToolFunctionModel>[];

    if (await ToolValidator.isValidOpenToolJson(tool.schemaText)) {
      final schemaMap = jsonDecode(tool.schemaText) as Map<String, dynamic>;
      final functionArray = schemaMap["functions"] as List;

      for (final json in functionArray) {
        final functionData = json as Map<String, dynamic>;
        functions.add(ToolFunctionModel()
          ..toolId = tool.id
          ..toolName = tool.name
          ..functionName = functionData["name"] as String
          ..functionDescription = functionData["description"] as String
          ..isThirdTool = true);
      }
    }

    return functions;
  }

  /// 解析 OpenAPI 工具函数
  static Future<List<ToolFunctionModel>> _parseOpenAPIFunctions(ToolModel tool) async {
    final functions = <ToolFunctionModel>[];
    String schema = "";

    if (await ToolValidator.isValidOpenAIJson(tool.schemaText)) {
      schema = tool.schemaText;
    } else if (await ToolValidator.isValidOpenAIYaml(tool.schemaText)) {
      final map = loadYaml(tool.schemaText) as YamlMap;
      schema = jsonEncode(map);
    }

    final schemaMap = jsonDecode(schema) as Map<String, dynamic>;
    final paths = schemaMap["paths"] as Map<String, dynamic>;

    paths.forEach((functionName, value) {
      final pathData = value as Map<String, dynamic>;
      pathData.forEach((method, methodData) {
        final methodInfo = methodData as Map<String, dynamic>;
        functions.add(ToolFunctionModel()
          ..toolId = tool.id
          ..toolName = tool.name
          ..functionName = functionName
          ..requestMethod = method
          ..functionDescription = methodInfo["summary"] as String? ?? "");
      });
    });

    return functions;
  }

  /// 解析 JSON-RPC 工具函数
  static Future<List<ToolFunctionModel>> _parseJsonRPCFunctions(ToolModel tool) async {
    final functions = <ToolFunctionModel>[];

    if (await ToolValidator.isValidOpenPPCJson(tool.schemaText)) {
      final schemaMap = jsonDecode(tool.schemaText) as Map<String, dynamic>;
      final methods = schemaMap["methods"] as List;

      for (final method in methods) {
        final methodData = method as Map<String, dynamic>;
        functions.add(ToolFunctionModel()
          ..toolId = tool.id
          ..toolName = tool.name
          ..functionName = methodData["name"] as String
          ..functionDescription = methodData["description"] as String);
      }
    }

    return functions;
  }

  /// 从OpenTool服务器获取最新的OpenTool信息
  static Future<OpenTool?> loadOpenToolFromServer(String url, String? apiKey) async {
    try {
      if (url.isNotEmpty) {
        final uri = Uri.parse(url);
        final client = OpenToolClient(
          isSSL: uri.scheme == 'https',
          host: uri.host,
          port: uri.port > 0 ? uri.port : (uri.scheme == 'https' ? 443 : 80),
          apiKey: apiKey,
        );

        return await client.load();
      }
    } catch (e) {
      // 服务器获取失败，返回null
    }

    return null;
  }

  /// 从OpenTool服务器配置获取最新的OpenTool信息
  static Future<OpenTool?> loadOpenToolFromConfig(Map<String, dynamic> config) async {
    try {
      String? url = config["serverUrl"] as String?;
      final apiKey = config["apiKey"] as String?;

      if (url == null || url.isEmpty) {
        // 如果serverUrl为空，尝试使用host和port拼接
        final host = config["host"] as String?;
        final port = config["port"] as int?;
        if (host != null && host.isNotEmpty) {
          final portStr = port != null ? ":$port" : "";
          url = "$host$portStr"; // host自带协议，直接拼接
        }
      }

      if (url != null && url.isNotEmpty) {
        return await loadOpenToolFromServer(url, apiKey);
      }
    } catch (e) {
      // 配置解析失败，返回null
    }

    return null;
  }

  /// 解析 OpenTool Server 工具函数
  static Future<List<ToolFunctionModel>> _parseOpenToolServerFunctions(ToolModel tool) async {
    final functions = <ToolFunctionModel>[];

    if (await ToolValidator.isValidOpenToolServerJson(tool.schemaText)) {
      final schemaMap = jsonDecode(tool.schemaText) as Map<String, dynamic>;

      OpenTool? openTool;

      // 尝试从服务器获取最新信息
      openTool = await loadOpenToolFromConfig(schemaMap);

      if (openTool == null) {
        final schemaStr = schemaMap["schema"] as String;
        openTool = OpenTool.fromJson(jsonDecode(schemaStr));
      }

      // 统一处理OpenTool对象
      if (openTool.functions.isNotEmpty) {
        for (final function in openTool.functions) {
          functions.add(ToolFunctionModel()
            ..toolId = tool.id
            ..toolName = tool.name
            ..functionName = function.name
            ..functionDescription = function.description
            ..isThirdTool = false);
        }
      }
    }

    return functions;
  }

  static String generateFunctionIdByFunction(ToolFunctionModel function) {
    return _generateFunctionId(function.requestMethod ?? "", function.functionName);
  }

  static String generateFunctionIdByFunctionDTO(FunctionDto function) {
    return _generateFunctionId(function.requestMethod, function.functionName);
  }

  static String _generateFunctionId(String requestMethod, String functionName) {
    var requestMethodString = requestMethod.isEmpty?"null":requestMethod.toLowerCase();
    final input = '$requestMethodString:$functionName';
    return input.encodeBase64();
  }
}
