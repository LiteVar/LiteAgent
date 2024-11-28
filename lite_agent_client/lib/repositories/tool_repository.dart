import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/dto/tool_detail.dart';
import 'package:lite_agent_client/server/api_server/tool_server.dart';

import '../models/local_data_model.dart';

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
      var dto = ToolDTO(tool.id, "", "", tool.name, tool.description, 0, tool.schemaText, tool.apiText, tool.apiType, false, "", "");
      list.add(dto);
    }
    return list;
  }

  Future<void> removeTool(String key) async {
    await (await _toolBox).delete(key);
  }

  Future<void> updateTool(String key, ToolBean tool) async {
    await (await _toolBox).put(key, tool);
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

  Future<ToolDetailDTO?> getCloudToolDetail(String id) async {
    var response = await ToolServer.getToolDetail(id);
    return response.data;
  }
}
