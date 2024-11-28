import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/agent_detail.dart';
import 'package:lite_agent_client/server/api_server/agent_server.dart';

import '../models/dto/chat.dart';
import '../models/local_data_model.dart';

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
    List<AgentDTO> list = [];
    var iterable = (await _agentBox).values.iterator;
    while (iterable.moveNext()) {
      var agent = iterable.current;
      var dto = agent.translateToDTO();
      list.add(dto);
    }
    list.sort((a, b) {
      if (a.id.isEmpty || b.id.isEmpty) {
        return 0;
      }
      return double.parse(b.id).toInt() - double.parse(a.id).toInt();
    });
    return list;
  }

  Future<void> removeAgent(String key) async {
    await (await _agentBox).delete(key);
  }

  Future<void> updateAgent(String key, AgentBean agent) async {
    await (await _agentBox).put(key, agent);
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
}
