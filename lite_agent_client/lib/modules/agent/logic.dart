import 'dart:async';

import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/routes.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/uitl/snowflake_uitl.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_detail.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_edit.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/local_data_model.dart';
import '../../repositories/model_repository.dart';
import '../../utils/alarm_util.dart';
import '../../widgets/dialog/dialog_common_confirm.dart';
import '../../widgets/dialog/dialog_login.dart';

class AgentLogic extends GetxController with WindowListener {
  static const String TAB_LOCAL = "local";
  static const String TAB_CLOUD = "cloud";
  static const String TAB_SEC_ALL = "secondary_all";
  static const String TAB_SEC_SYSTEM = "secondary_system";
  static const String TAB_SEC_SHARE = "secondary_share";
  static const String TAB_SEC_MINE = "secondary_mine";

  late StreamSubscription _subscription;
  late StreamSubscription _agentSubscription;

  var currentTab = TAB_LOCAL.obs;
  var currentSecondaryTab = TAB_SEC_ALL.obs;

  var currentAgentList = <AgentDTO>[].obs;
  final Map<String, List<AgentDTO>> _agentListMap = <String, List<AgentDTO>>{};

  var isLogin = false;

  @override
  void onInit() {
    super.onInit();
    initWindow();
    initData();
    initEventBus();
  }

  void initWindow() async {
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  void initData() async {
    isLogin = (await accountRepository.isLogin());
    _agentListMap[TAB_LOCAL] = <AgentDTO>[];
    _agentListMap[TAB_SEC_ALL] = <AgentDTO>[];
    _agentListMap[TAB_SEC_SYSTEM] = <AgentDTO>[];
    _agentListMap[TAB_SEC_SHARE] = <AgentDTO>[];
    _agentListMap[TAB_SEC_MINE] = <AgentDTO>[];
    loadLocalData();
    loadCloudData();
    switchTab(TAB_LOCAL);
  }

  Future<void> loadLocalData() async {
    _agentListMap[TAB_LOCAL]!.assignAll((await agentRepository.getAgentDTOListFromBox()));
    currentAgentList.refresh();
  }

  Future<void> loadCloudData() async {
    _agentListMap[TAB_SEC_ALL]!.assignAll((await agentRepository.getCloudAgentList(0)));
    _agentListMap[TAB_SEC_SYSTEM]!.assignAll((await agentRepository.getCloudAgentList(1)));
    _agentListMap[TAB_SEC_SHARE]!.assignAll((await agentRepository.getCloudAgentList(2)));
    _agentListMap[TAB_SEC_MINE]!.assignAll((await agentRepository.getCloudAgentList(3)));

    currentAgentList.refresh();
  }

  void initEventBus() {
    _subscription = eventBus.on<MessageEvent>().listen((event) {
      if (event.message == EventBusMessage.login) {
        isLogin = true;
        currentTab.refresh();
        loadCloudData();
      } else if (event.message == EventBusMessage.logout) {
        isLogin = false;
        currentTab.refresh();
      }
    });
    _agentSubscription = eventBus.on<AgentMessageEvent>().listen((event) {
      if (event.message == EventBusMessage.updateList) {
        loadLocalData();
      } else if (event.message == EventBusMessage.updateSingleData && event.agent != null) {
        var agent = event.agent;
        if (agent != null) {
          updateAgent(agent, false);
        }
      }
    });
  }

  @override
  void onClose() {
    _subscription.cancel();
    _agentSubscription.cancel();
    super.onClose();
  }

  @override
  void onWindowClose() {
    Get.back();
  }

  void removeAgent(String id) {
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
          title: "删除确认",
          content: "即将删除agent的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () async {
            var localList = _agentListMap[TAB_LOCAL];
            if (localList == null) {
              return;
            }
            for (var agent in localList) {
              if (agent.id == id) {
                localList.remove(agent);
                agentRepository.removeAgent(id);
                currentAgentList.refresh();
                break;
              }
            }
          },
        ));
  }

  void createNewAgent(String name, String iconPath, String description) async {
    var localList = _agentListMap[TAB_LOCAL];
    if (localList == null) {
      return;
    }
    String agentId = snowFlakeUtil.getId();
    int createTime = DateTime.now().microsecondsSinceEpoch;
    var agent = AgentDTO(agentId, "", "", name, iconPath, description, "", "", <String>[], 0, false, 0.0, 1.0, 4096, "", "", null, null,
        null, null, null, false);
    localList.insert(0, agent);
    currentAgentList.refresh();

    AgentBean targetAgent = AgentBean();
    targetAgent.translateFromDTO(agent);
    targetAgent.createTime = createTime;
    await agentRepository.updateAgent(agentId, targetAgent);
  }

  void updateAgent(AgentBean agent, bool updateBox) async {
    var localList = _agentListMap[TAB_LOCAL];
    if (localList == null) {
      return;
    }
    String targetId = agent.id;
    if (targetId.isNotEmpty) {
      for (var target in localList) {
        if (target.id == targetId) {
          var index = localList.indexOf(target);
          localList[index] = agent.translateToDTO();
          currentAgentList.refresh();
          if (updateBox) {
            await agentRepository.updateAgent(targetId, agent);
          }
          break;
        }
      }
    }
  }

  void switchTab(String tabType) {
    if (tabType == TAB_LOCAL || tabType == TAB_CLOUD) {
      currentTab.value = tabType;
    } else {
      currentSecondaryTab.value = tabType;
    }
    if (currentTab.value == TAB_LOCAL) {
      currentAgentList.value = _agentListMap[TAB_LOCAL]!;
    } else {
      currentAgentList.value = _agentListMap[currentSecondaryTab.value]!;
    }
  }

  void jumpToAdjustPage(AgentDTO agent) {
    AgentBean targetAgent = AgentBean();
    targetAgent.translateFromDTO(agent);
    Get.toNamed(Routes.adjustment, arguments: targetAgent);
  }

  void showCreateAgentDialog() {
    Get.dialog(
        barrierDismissible: false,
        EditAgentDialog(
            name: "",
            iconPath: "",
            description: "",
            isEdit: false,
            onConfirmCallback: (name, iconPath, description) {
              createNewAgent(name, iconPath, description);
              switchTab(TAB_LOCAL);
            }));
  }

  void showAgentDetailDialog(AgentDTO agent) {
    AgentBean targetAgent = AgentBean();
    targetAgent.translateFromDTO(agent);
    Get.dialog(AgentDetailDialog(agent: targetAgent));
  }

  void startChat(AgentDTO agent) async {
    if (agent.isCloud ?? false) {
      var agentDetail = await agentRepository.getCloudAgentDetail(agent.id);
      if (agentDetail?.model == null) {
        AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
        return;
      }
      if (agentDetail?.agent?.type == AgentType.REFLECTION) {
        AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
        return;
      }
    } else {
      String modelId = agent.llmModelId ?? "";
      var model = await modelRepository.getModelFromBox(modelId);
      if (model == null) {
        AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
        return;
      }
      if (agent.type == AgentType.REFLECTION) {
        AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
        return;
      }
    }
    AgentBean targetAgent = AgentBean();
    targetAgent.translateFromDTO(agent);
    eventBus.fire(AgentMessageEvent(message: EventBusMessage.startChat, agent: targetAgent));
  }

  void onLoginButtonClick() {
    Get.dialog(barrierDismissible: false, LoginDialog());
  }

  void onRefreshButtonClick() async {
    if (currentTab.value == TAB_CLOUD) {
      EasyLoading.show(status: "加载中...");
      try {
        await loadCloudData();
        EasyLoading.dismiss();
        AlarmUtil.showAlertToast("同步成功");
        eventBus.fire(MessageEvent(message: EventBusMessage.sync));
      } catch (e) {
        EasyLoading.dismiss();
      }
    }
  }
}
