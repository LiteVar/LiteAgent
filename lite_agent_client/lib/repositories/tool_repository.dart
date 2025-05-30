import 'dart:convert';

import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/server/api_server/tool_server.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

import '../models/local_data_model.dart';
import 'account_repository.dart';

final toolRepository = ToolRepository();

class ToolRepository {
  static final ToolRepository _instance = ToolRepository._internal();

  factory ToolRepository() => _instance;

  ToolRepository._internal();

  static const String toolBoxKey = "tool_box_key";
  Box<ToolBean>? _box;

  Future<Box<ToolBean>> get _toolBox async => _box ??= await Hive.openBox<ToolBean>(toolBoxKey);

  Future<Iterable<ToolBean>> getToolListFromBox() async {
    return (await _toolBox).values;
  }

  Future<List<ToolDTO>> getToolDTOListFromBox() async {
    List<ToolDTO> list = [];
    var iterable = (await _toolBox).values.iterator;
    while (iterable.moveNext()) {
      var tool = iterable.current;
      var dto = tool.translateToDTO();
      list.add(dto);
    }
    return list;
  }

  Future<void> removeTool(String key) async {
    await (await _toolBox).delete(key);
    if (await accountRepository.isLogin()) {
      await ToolServer.removeTool(key);
    }
  }

  Future<void> updateTool(String key, ToolBean tool) async {
    await (await _toolBox).put(key, tool);
    if (await accountRepository.isLogin()) {
      await uploadToServer([tool]);
    }
    print("updateTool:$key");
  }

  Future<ToolBean?> getToolFromBox(String key) async {
    return (await _toolBox).get(key);
  }

  Future<void> clear() async {
    await (await _toolBox).clear();
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

  Future<void> uploadToServer(List<ToolBean> tools) async {
    var list = <ToolDTO>[];
    for (var tool in tools) {
      list.add(tool.translateToDTO());
    }
    list.removeWhere((tool) {
      var schemaStr = tool.schemaStr ?? "";
      if (schemaStr.isEmpty) {
        return true;
      }
      return !schemaStr.isJson() && !schemaStr.isYaml();
    });
    String jsonString = json.encode(list);
    List<dynamic> jsonArray = json.decode(jsonString);
    //print("jsonString:$jsonString");
    var response = await ToolServer.toolSync(jsonArray);
    if (response.code == 200) {
      print("toolUploadServer:${list.length}");
    }
  }

  Future<void> uploadAllToServer() async {
    var tools = <ToolBean>[];
    tools.addAll(((await _toolBox).values));
    uploadToServer(tools);
  }
}
