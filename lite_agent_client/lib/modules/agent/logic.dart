import 'dart:async';

import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/routes.dart';
import 'package:lite_agent_client/utils/agent/agent_validator.dart';
import 'package:lite_agent_client/utils/snowflake_util.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_detail.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_edit.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/local/agent.dart';
import '../../repositories/model_repository.dart';
import '../../utils/alarm_util.dart';
import '../../widgets/dialog/dialog_common_confirm.dart';
import '../../widgets/dialog/dialog_login.dart';
import '../home/logic.dart';

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

  var currentAgentList = <AgentModel>[].obs;
  final _agentListMap = <String, List<AgentModel>>{};

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
    _agentListMap[TAB_LOCAL] = <AgentModel>[];
    _agentListMap[TAB_SEC_ALL] = <AgentModel>[];
    _agentListMap[TAB_SEC_SYSTEM] = <AgentModel>[];
    _agentListMap[TAB_SEC_SHARE] = <AgentModel>[];
    _agentListMap[TAB_SEC_MINE] = <AgentModel>[];
    loadLocalData();
    loadAllCloudData();
    switchTab(TAB_LOCAL);
  }

  Future<void> loadLocalData() async {
    List<AgentModel> list = [];
    list.assignAll((await agentRepository.getAgentListFromBox()));
    list.sort((a, b) => (b.createTime ?? 0) - (a.createTime ?? 0));
    _agentListMap[TAB_LOCAL]!.assignAll(list);

    AgentModel? autoAgent = _agentListMap[TAB_LOCAL]!.firstWhereOrNull((agent) => agent.autoAgentFlag == true);
    if (autoAgent != null) {
      _agentListMap[TAB_LOCAL]!.remove(autoAgent);
      _agentListMap[TAB_LOCAL]!.insert(0, autoAgent);
    } else {
      _agentListMap[TAB_LOCAL]!.insert(0, await newAutoAgent());
    }

    currentAgentList.refresh();
  }

  Future<void> loadAllCloudData() async {
    _agentListMap[TAB_SEC_ALL]!.assignAll((await agentRepository.getCloudAgentListAndTranslate(0)));
    _agentListMap[TAB_SEC_SYSTEM]!.assignAll((await agentRepository.getCloudAgentListAndTranslate(1)));
    _agentListMap[TAB_SEC_SHARE]!.assignAll((await agentRepository.getCloudAgentListAndTranslate(2)));
    _agentListMap[TAB_SEC_MINE]!.assignAll((await agentRepository.getCloudAgentListAndTranslate(3)));

    currentAgentList.refresh();
  }

  void initEventBus() {
    _subscription = eventBus.on<MessageEvent>().listen((event) {
      if (event.message == EventBusMessage.login) {
        isLogin = true;
        currentTab.refresh();
        loadAllCloudData();
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
      } else if (event.message == EventBusMessage.delete && event.agent != null) {
        _removeAgent(event.agent?.id ?? "");
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

  Future<AgentModel> newAutoAgent() async {
    String des = "AI能够理解任务，并从工具库和模型库中，搭建一个临时的agent执行任务，可以精准、高效地达成目标。";
    String agentId = snowFlakeUtil.getId();
    int createTime = DateTime.now().microsecondsSinceEpoch;
    var autoAgent =
        AgentModel(id: agentId, name: "Auto Multi Agent", iconPath: "", description: des, createTime: createTime, autoAgentFlag: true);
    await agentRepository.updateAgent(autoAgent.id, autoAgent);
    return autoAgent;
  }

  void showRemoveAgentDialog(String id) {
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
          title: "删除确认",
          content: "即将删除agent的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () => _removeAgent(id),
        ));
  }

  Future<void> _removeAgent(String id) async {
    var localList = _agentListMap[TAB_LOCAL];
    if (localList == null) {
      return;
    }
    for (var agent in localList) {
      if (agent.id == id) {
        localList.remove(agent);
        agentRepository.removeAgent(id);
        eventBus.fire(AgentMessageEvent(message: EventBusMessage.delete, agent: AgentModel.onlyId(id)));
        currentAgentList.refresh();
        break;
      }
    }
  }

  void createNewAgent(String name, String iconPath, String description) async {
    var localList = _agentListMap[TAB_LOCAL];
    if (localList == null) {
      return;
    }
    String agentId = snowFlakeUtil.getId();
    int createTime = DateTime.now().microsecondsSinceEpoch;
    var newAgent = AgentModel(id: agentId, name: name, iconPath: iconPath, description: description, createTime: createTime);
    localList.insert(1, newAgent);
    currentAgentList.refresh();
    await agentRepository.updateAgent(newAgent.id, newAgent);
  }

  void updateAgent(AgentModel agent, bool updateBox) async {
    var localList = _agentListMap[TAB_LOCAL]!;
    String targetId = agent.id;
    if (targetId.isNotEmpty) {
      for (var target in localList) {
        if (target.id == targetId) {
          var index = localList.indexOf(target);
          localList[index] = agent;
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

  void jumpToAdjustPage(AgentModel agent) {
    Get.toNamed(Routes.adjustment, arguments: agent);
  }

  void showCreateAgentDialog() {
    Get.dialog(
        barrierDismissible: false,
        EditAgentDialog(
            agent: null,
            isEdit: false,
            onConfirmCallback: (name, iconPath, description) {
              createNewAgent(name, iconPath, description);
              switchTab(TAB_LOCAL);
            }));
  }

  void showAgentDetailDialog(AgentModel agent) {
    Get.dialog(AgentDetailDialog(agent: agent));
  }

  void startChat(AgentModel agent) async {
    var isCloudAgent = agent.isCloud ?? false;
    if (isCloudAgent) {
      var agentDetail = await agentRepository.getCloudAgentDetail(agent.id);
      if (agentDetail?.model == null) {
        AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
        return;
      }
      if (agentDetail?.agent?.type == AgentValidator.DTO_TYPE_REFLECTION) {
        AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
        return;
      }
    } else {
      var model = await modelRepository.getModelFromBox(agent.modelId);
      if (model == null) {
        AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
        return;
      }
      if (agent.agentType == AgentValidator.DTO_TYPE_REFLECTION) {
        AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
        return;
      }
    }
    eventBus.fire(AgentMessageEvent(message: EventBusMessage.startChat, agent: agent));
  }

  void onLoginButtonClick() {
    Get.dialog(barrierDismissible: false, LoginDialog());
  }

  void onRefreshButtonClick() async {
    if (currentTab.value == TAB_CLOUD) {
      EasyLoading.show(status: "加载中...");
      try {
        await loadAllCloudData();
        EasyLoading.dismiss();
        AlarmUtil.showAlertToast("同步成功");
        eventBus.fire(MessageEvent(message: EventBusMessage.sync));
      } catch (e) {
        EasyLoading.dismiss();
      }
    }
  }

  void showImportAgentDialog() {
    // 通过 HomePageLogic 切换到导入页面
    if (Get.isRegistered<HomePageLogic>()) {
      final homeLogic = Get.find<HomePageLogic>();
      homeLogic.switchToAgentImportPage();
    }
  }
}
