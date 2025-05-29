import 'dart:convert';

import 'package:get/get.dart';
import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/agent_detail.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/server/api_server/agent_server.dart';

import '../models/dto/chat.dart';
import '../models/local_data_model.dart';
import 'model_repository.dart';

final agentRepository = AgentRepository();

class AgentRepository {
  static final AgentRepository _instance = AgentRepository._internal();

  factory AgentRepository() => _instance;

  AgentRepository._internal();

  static const String agentBoxKey = "agent_box_key";
  Box<AgentBean>? _box;

  Future<Box<AgentBean>> get _agentBox async => _box ??= await Hive.openBox<AgentBean>(agentBoxKey);

  Future<Iterable<AgentBean>> getAgentListFromBox() async {
    return (await _agentBox).values;
  }

  Future<List<AgentDTO>> getAgentDTOListFromBox() async {
    List<AgentBean> list = [];
    list.assignAll((await _agentBox).values);
    list.sort((a, b) => (b.createTime ?? 0) - (a.createTime ?? 0));
    List<AgentDTO> dtoList = [];
    for (var agent in list) {
      var dto = agent.translateToDTO();
      dtoList.add(dto);
    }
    return dtoList;
  }

  Future<void> removeAgent(String key) async {
    await (await _agentBox).delete(key);
    if (await accountRepository.isLogin()) {
      await AgentServer.removeAgent(key);
    }
  }

  Future<void> updateAgent(String key, AgentBean agent) async {
    await (await _agentBox).put(key, agent);
    if (await accountRepository.isLogin()) {
      await uploadToServer([agent]);
    }
    print("updateAgent:$key");
  }

  Future<AgentBean?> getAgentFromBox(String key) async {
    return (await _agentBox).get(key);
  }

  Future<void> clear() async {
    await (await _agentBox).clear();
  }

  Future<List<AgentDTO>> getCloudAgentList(int tab) async {
    List<AgentDTO> list = [];
    var response = await AgentServer.getAgentList(tab);
    if (response.data != null) {
      list.addAll(response.data!);
      list.forEach((element) => element.isCloud = true);
    }
    return list;
  }

  Future<AgentDetailDTO?> getCloudAgentDetail(String agentId) async {
    var response = await AgentServer.getAgentDetail(agentId);
    return response.data;
  }

  Future<List<ChatDTO>> getCloudAgentConversationList() async {
    List<ChatDTO> list = [];
    var response = await AgentServer.getAgentConversationList();
    if (response.data != null) {
      list.addAll(response.data!);
    }
    return list;
  }

  Future<void> uploadToServer(List<AgentBean> agents) async {
    var list = <AgentDTO>[];
    for (var agent in agents) {
      var dto = agent.translateToDTO();
      dto.icon = "";
      var model = await modelRepository.getModelFromBox(agent.modelId);
      if (model == null) {
        dto.llmModelId = "";
      }
      list.add(dto);
    }
    String jsonString = json.encode(list);
    List<dynamic> jsonArray = json.decode(jsonString);
    //print("jsonString:$jsonString");
    var response = await AgentServer.agentSync(jsonArray);
    if (response.code == 200) {
      print("agentUploadServer:${list.length}");
    }
  }

  Future<void> uploadAllToServer() async {
    var agents = <AgentBean>[];
    agents.addAll(((await _agentBox).values));
    uploadToServer(agents);
  }
}
