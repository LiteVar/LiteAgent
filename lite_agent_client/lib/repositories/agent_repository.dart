import 'dart:convert';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/agent_detail.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/server/api_server/agent_server.dart';
import 'package:lite_agent_client/utils/extension/agent_extension.dart';

import '../utils/log_util.dart';
import 'base_hive_repository.dart';
import 'model_repository.dart';

final agentRepository = AgentRepository();

class AgentRepository extends BaseHiveRepository<AgentModel> {
  static final AgentRepository _instance = AgentRepository._internal();

  factory AgentRepository() => _instance;

  AgentRepository._internal() : super("agent_box_key");

  Future<Iterable<AgentModel>> getAgentListFromBox() async => getAll();

  Future<void> removeAgent(String key) async {
    await delete(key);
    if (await accountRepository.isLogin()) {
      await AgentServer.removeAgent(key);
    }
  }

  Future<void> updateAgent(String key, AgentModel agent) async {
    await save(key, agent);
    if (await accountRepository.isLogin()) {
      await uploadToServer([agent]);
    }
  }

  Future<void> updateAgents(Map<String, AgentModel> agents) async {
    await saveAll(agents);
    if (await accountRepository.isLogin()) {
      await uploadToServer(agents.values.toList());
    }
  }

  Future<AgentModel?> getAgentFromBox(String key) async => getData(key);

  Future<List<AgentDTO>> getCloudAgentList(int tab) async {
    List<AgentDTO> list = [];
    var response = await AgentServer.getAgentList(tab);
    if (response.data != null) {
      list.addAll(response.data!);
      //list.removeWhere((element) => element.autoAgentFlag ?? false);
      list.forEach((element) => element.isCloud = true);
    }
    return list;
  }

  Future<List<AgentModel>> getCloudAgentListAndTranslate(int tab) async {
    var list = await agentRepository.getCloudAgentList(tab);
    var agentList = <AgentModel>[];
    for (var item in list) {
      var agent = item.toModel();
      agentList.add(agent);
    }
    return agentList;
  }

  Future<AgentDetailDTO?> getCloudAgentDetail(String agentId) async {
    var response = await AgentServer.getAgentDetail(agentId);
    return response.data;
  }

  Future<void> uploadToServer(List<AgentModel> agents) async {
    var list = <AgentDTO>[];
    for (var agent in agents) {
      if (!agent.id.isNumericOnly) {
        continue;
      }
      var dto = agent.toDTO();
      dto.icon = "";
      var model = await modelRepository.getModelFromBox(agent.modelId);
      if (model == null) {
        dto.llmModelId = "";
      }
      list.add(dto);
    }
    String jsonString = json.encode(list);
    List<dynamic> jsonArray = json.decode(jsonString);
    var response = await AgentServer.agentSync(jsonArray);
    if (response.code == 200) {
      Log.i("agentUploadServer:${list.length}");
    }
  }

  Future<void> uploadAllToServer() async {
    var agents = <AgentModel>[];
    agents.addAll(await getAll());
    uploadToServer(agents);
  }
}
