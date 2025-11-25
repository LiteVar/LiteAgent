import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/utils/agent/agent_validator.dart';
import 'package:lite_agent_client/config/constants.dart';

/// Agent状态管理器
/// 负责管理Agent相关的所有状态数据
class AgentStateManager {
  // Agent参数
  final RxDouble sliderTempValue = 0.0.obs;
  final RxDouble sliderTokenValue = 4096.0.obs;
  final RxDouble sliderTopPValue = 1.0.obs;
  
  // Agent类型和执行模式
  final RxInt agentType = AgentValidator.DTO_TYPE_GENERAL.obs;
  final RxInt operationMode = OperationMode.PARALLEL.obs;
  
  // 子Agent
  final RxList<AgentModel> childAgentList = <AgentModel>[].obs;
  
  // UI状态
  final RxBool isChildAgentsExpanded = false.obs;
  final RxBool isExecutionModeExpanded = false.obs;
  final RxString agentHoverItemId = "".obs;


  void toggleChildAgentsExpanded(bool expanded) {
    isChildAgentsExpanded.value = expanded;
  }

  void toggleExecutionModeExpanded(bool expanded) {
    isExecutionModeExpanded.value = expanded;
  }

  void hoverAgentItem(String id) {
    agentHoverItemId.value = id;
  }

  void updateAgentType(int type) {
    agentType.value = type;
  }

  void updateOperationMode(int mode) {
    operationMode.value = mode;
  }

  void updateTemperature(double value) {
    sliderTempValue.value = value;
  }

  void updateMaxToken(double value) {
    sliderTokenValue.value = value;
  }

  void updateTopP(double value) {
    sliderTopPValue.value = value;
  }

  void addChildAgent(AgentModel agent) {
    childAgentList.add(agent);
  }

  void removeChildAgent(int index) {
    if (index >= 0 && index < childAgentList.length) {
      childAgentList.removeAt(index);
    }
  }

  void clearChildAgents() {
    childAgentList.clear();
  }

}
