import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/routes.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_detail.dart';
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
    loadLocalData(false);
    loadCloudData(false);
    switchTab(TAB_LOCAL);
  }

  Future<void> loadLocalData(bool refresh) async {
    _agentListMap[TAB_LOCAL]!.assignAll((await agentRepository.getAgentDTOListFromBox()));
    if (refresh) {
      currentAgentList.refresh();
    }
  }

  Future<void> loadCloudData(bool refresh) async {
    _agentListMap[TAB_SEC_ALL]!.assignAll((await agentRepository.getCloudAgentList(0)));
    _agentListMap[TAB_SEC_SYSTEM]!.assignAll((await agentRepository.getCloudAgentList(1)));
    _agentListMap[TAB_SEC_SHARE]!.assignAll((await agentRepository.getCloudAgentList(2)));
    _agentListMap[TAB_SEC_MINE]!.assignAll((await agentRepository.getCloudAgentList(3)));
    if (refresh) {
      currentAgentList.refresh();
    }
  }

  void initEventBus() {
    _subscription = eventBus.on<MessageEvent>().listen((event) {
      if (event.message == EventBusMessage.login) {
        isLogin = true;
        currentTab.refresh();
        loadCloudData(true);
      } else if (event.message == EventBusMessage.logout) {
        isLogin = false;
        currentTab.refresh();
      }
    });
    _agentSubscription = eventBus.on<AgentMessageEvent>().listen((event) {
      if (event.message == EventBusMessage.updateList) {
        loadLocalData(true);
      } else if (event.message == EventBusMessage.updateSingleData && event.agent != null) {
        var agent = event.agent;
        if (agent != null) {
          updateAgent(agent.id, agent.name, agent.iconPath, agent.description, false);
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

  void updateAgent(String id, String name, String iconPath, String description, bool updateBox) async {
    var localList = _agentListMap[TAB_LOCAL];
    if (localList == null) {
      return;
    }
    String targetId = id;
    if (targetId.isNotEmpty) {
      for (var agent in localList) {
        if (agent.id == targetId) {
          agent.name = name;
          agent.icon = iconPath;
          agent.description = description;
          break;
        }
      }
    } else {
      targetId = DateTime.now().microsecondsSinceEpoch.toString();
      var agent = AgentDTO(targetId, "", "", name, iconPath, description, "", "", <String>[], 0, false, 0.0, 1.0, 4096, "", "");
      localList.insert(0, agent);
    }
    currentAgentList.refresh();

    if (updateBox) {
      AgentBean targetAgent = AgentBean();
      targetAgent.id = targetId;
      targetAgent.name = name;
      targetAgent.iconPath = iconPath;
      targetAgent.description = description;
      await agentRepository.updateAgent(targetAgent.id, targetAgent);
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
    showEditAgentDialog(null);
  }

  void showEditAgentDialog(AgentBean? bean) {
    String agentId = bean?.id ?? "";
    Get.dialog(
        barrierDismissible: false,
        EditAgentDialog(
            name: bean?.name ?? "",
            iconPath: bean?.iconPath ?? "",
            description: bean?.description ?? "",
            isEdit: agentId.isNotEmpty,
            onConfirmCallback: (name, iconPath, description) {
              updateAgent(agentId, name, iconPath, description, true);
              switchTab(TAB_LOCAL);
            }));
  }

  void showAgentDetailDialog(AgentDTO agent) {
    AgentBean targetAgent = AgentBean();
    targetAgent.translateFromDTO(agent);
    Get.dialog(AgentDetailDialog(agent: targetAgent));
  }

  void startChat(AgentDTO agent) async {
    String modelId = agent.llmModelId ?? "";
    var model = await modelRepository.getModelFromBox(modelId);
    if (model == null) {
      AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
      return;
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
      await loadCloudData(true);
      EasyLoading.dismiss();
    }
  }
}
