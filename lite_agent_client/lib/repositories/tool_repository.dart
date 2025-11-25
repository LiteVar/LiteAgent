import 'dart:convert';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/open_tool_schema.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/server/api_server/tool_server.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/extension/tool_extension.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';

import '../utils/log_util.dart';
import 'account_repository.dart';
import 'base_hive_repository.dart';

final toolRepository = ToolRepository();

class ToolRepository extends BaseHiveRepository<ToolModel> {
  static final ToolRepository _instance = ToolRepository._internal();

  factory ToolRepository() => _instance;

  ToolRepository._internal() : super("tool_box_key");

  Future<Iterable<ToolModel>> getToolListFromBox() async {
    return (await getAll()).where((tool) => ToolValidator.isSchemaTypeSupported(tool.schemaType));
  }

  Future<List<ToolModel>> getCloudAgentListAndTranslate(int tab) async {
    var list = await toolRepository.getCloudToolList(tab);
    var tooList = <ToolModel>[];
    for (var item in list) {
      var tool = item.toModel();
      tool.isCloud = true;
      tooList.add(tool);
    }
    return tooList;
  }

  Future<void> removeTool(String key) async {
    await delete(key);
    if (await accountRepository.isLogin()) {
      await ToolServer.removeTool(key);
    }
  }

  Future<void> updateTool(String key, ToolModel tool) async {
    await save(key, tool);
    if (await accountRepository.isLogin()) {
      await uploadToServer([tool]);
    }
  }

  Future<ToolModel?> getToolFromBox(String key) async {
    ToolModel? tool = await getData(key);
    if (tool != null && !ToolValidator.isSchemaTypeSupported(tool.schemaType)) {
      return null;
    }
    return tool;
  }

  Future<List<ToolDTO>> getCloudToolList(int tab) async {
    List<ToolDTO> list = [];
    var response = await ToolServer.getTooList(tab);
    if (response.data != null) {
      list.addAll(response.data!);
    }
    return list;
  }

  Future<ToolDTO?> getCloudToolDetail(String id) async {
    var response = await ToolServer.getToolDetail(id);
    return response.data;
  }

  Future<void> uploadToServer(List<ToolModel> tools) async {
    var list = <ToolDTO>[];
    for (var tool in tools) {
      if (!tool.id.isNumericOnly) {
        continue;
      }
      list.add(tool.toDTO());
    }
    list.removeWhere((tool) {
      var schemaStr = tool.schemaStr;
      if (schemaStr.isEmpty) {
        return true;
      }
      return !schemaStr.isJson() && !schemaStr.isYaml();
    });
    String jsonString = json.encode(list);
    List<dynamic> jsonArray = json.decode(jsonString);
    var response = await ToolServer.toolSync(jsonArray);
    if (response.code == 200) {
      Log.i("toolUploadServer:${list.length}");
    }
  }

  Future<void> uploadAllToServer() async {
    var tools = <ToolModel>[];
    tools.addAll(await getToolListFromBox());
    tools.removeWhere((item) => item.schemaType == Protocol.MCP_STDIO);
    uploadToServer(tools);
  }

  Future<List<ToolDTO>> getCloudAutoAgentToolList() async {
    List<ToolDTO> list = [];
    var response = await ToolServer.getAutoAgentToolList();
    if (response.data != null) {
      list.addAll(response.data!);
    }
    return list;
  }

  Future<OpenToolSchemaDTO?> loadOpenToolSchema(String host, String apiKey) async {
    var response = await ToolServer.loadOpenToolSchema(host, apiKey);
    return response.data;
  }
}
