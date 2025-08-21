import 'dart:convert';

import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/server/local_server/mcp_service.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:yaml/yaml.dart' show YamlMap, loadYaml;
import 'package:lite_agent_core_dart/lite_agent_service.dart';

import '../widgets/dialog/dialog_tool_edit.dart';

class ToolSchemaParser {
  final McpService _mcpService;

  ToolSchemaParser(this._mcpService);

  static Future<List<AgentToolFunction>> parseFunctions(ToolBean tool, bool showLoading) async {
    if (tool.schemaText.isEmpty || tool.schemaType.isEmpty) {
      return [];
    }

    final parser = ToolSchemaParser(McpService());
    return await parser._parseFunctionsByType(tool, showLoading);
  }

  Future<List<AgentToolFunction>> _parseFunctionsByType(ToolBean tool, bool showLoading) async {
    switch (tool.schemaType) {
      case Protocol.MCP_STDIO_TOOLS:
        return await _parseMCPFunctions(tool, showLoading);
      case Protocol.OPENTOOL:
        return await _parseOpenToolFunctions(tool);
      case Protocol.JSONRPCHTTP:
      case SchemaType.JSONRPCHTTP:
        return await _parseJsonRPCFunctions(tool);
      case Protocol.OPENMODBUS:
      case SchemaType.OPENMODBUS:
        return await _parseModBusFunctions(tool);
      case Protocol.OPENAPI:
      case SchemaType.OPENAPI:
        return await _parseOpenAPIFunctions(tool);
      default:
        return [];
    }
  }

  Future<List<AgentToolFunction>> _parseMCPFunctions(ToolBean tool, bool showLoading) async {
    var functions = <AgentToolFunction>[];

    if (showLoading) {
      EasyLoading.show(status: "正在加载...");
    }
    try {
      var results = await _mcpService.checkServers(tool.schemaText);

      for (final result in results) {
        if (result.isAvailable && result.tools.isNotEmpty) {
          var serverName = result.serverId;
          for (final func in result.tools) {
            functions.add(AgentToolFunction()
              ..toolId = tool.id
              ..toolName = tool.name
              ..functionName = "$serverName/${func.name}"
              ..functionDescription = func.description);
          }
        }
      }
    } catch (e) {
      AlarmUtil.showAlertToast("MCP服务启动失败");
    } finally {
      if (showLoading) {
        EasyLoading.dismiss();
      }
    }

    return functions;
  }

  static Future<List<AgentToolFunction>> _parseOpenToolFunctions(ToolBean tool) async {
    var functions = <AgentToolFunction>[];

    if (await tool.schemaText.isOpenToolJson()) {
      Map<String, dynamic> schemaMap = jsonDecode(tool.schemaText);
      List functionArray = schemaMap["functions"];

      for (var json in functionArray) {
        functions.add(AgentToolFunction()
          ..toolId = tool.id
          ..toolName = tool.name
          ..functionName = json["name"]
          ..functionDescription = json["description"]
          ..isThirdTool = true);
      }
    }

    return functions;
  }

  static Future<List<AgentToolFunction>> _parseOpenAPIFunctions(ToolBean tool) async {
    var functions = <AgentToolFunction>[];
    String schema = "";

    if (await tool.schemaText.isOpenAIJson()) {
      schema = tool.schemaText;
    } else if (await tool.schemaText.isOpenAIYaml()) {
      YamlMap map = loadYaml(tool.schemaText);
      schema = jsonEncode(map);
    }

    Map<String, dynamic> schemaMap = jsonDecode(schema);
    schemaMap["paths"].forEach((key, value) {
      String functionName = key;
      value.forEach((key, value) {
        functions.add(AgentToolFunction()
          ..toolId = tool.id
          ..toolName = tool.name
          ..functionName = functionName
          ..requestMethod = key
          ..functionDescription = value["summary"]);
      });
    });

    return functions;
  }

  static Future<List<AgentToolFunction>> _parseJsonRPCFunctions(ToolBean tool) async {
    var functions = <AgentToolFunction>[];

    if (await tool.schemaText.isOpenPPCJson()) {
      Map<String, dynamic> schemaMap = jsonDecode(tool.schemaText);
      for (var method in schemaMap["methods"]) {
        functions.add(AgentToolFunction()
          ..toolId = tool.id
          ..toolName = tool.name
          ..functionName = method["name"]
          ..functionDescription = method["description"]);
      }
    }

    return functions;
  }

  static Future<List<AgentToolFunction>> _parseModBusFunctions(ToolBean tool) async {
    var functions = <AgentToolFunction>[];

    if (await tool.schemaText.isOpenModBusJson()) {
      Map<String, dynamic> schemaMap = jsonDecode(tool.schemaText);
      for (var func in schemaMap["functions"]) {
        functions.add(AgentToolFunction()
          ..toolId = tool.id
          ..toolName = tool.name
          ..functionName = func["name"]
          ..functionDescription = func["description"]);
      }
    }

    return functions;
  }
}
