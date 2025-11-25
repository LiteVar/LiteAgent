import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/models/local/message.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:window_manager/window_manager.dart';
import 'widgets/tool/tool_state_manager.dart';
import 'widgets/library/library_state_manager.dart';
import 'widgets/agent/agent_state_manager.dart';
import 'widgets/model/model_state_manager.dart';

import '../../models/local/agent.dart';
import '../../repositories/agent_repository.dart';
import '../../repositories/model_repository.dart';
import '../../utils/alarm_util.dart';
import '../../utils/log_util.dart';
import '../../utils/agent/agent_validator.dart';
import '../../utils/event_bus.dart';
import '../../utils/extension/agent_extension.dart';
import '../../modules/home/logic.dart';
import '../../widgets/dialog/dialog_model_edit.dart';
import 'services/chat_service.dart';
import 'services/agent_config_service.dart';

class AdjustmentLogic extends GetxController with WindowListener {
  Rx<AgentModel?> agent = Rx<AgentModel?>(null);
  var functionList = <ToolFunctionModel>[].obs;

  // 聊天服务
  late final ChatService chatService;

  // Agent配置服务
  late final AgentConfigService agentConfigService;

  // 工具状态管理器
  late final ToolStateManager toolStateManager;

  // 知识库状态管理器
  late final LibraryStateManager libraryStateManager;

  // Agent状态管理器
  late final AgentStateManager agentStateManager;

  // 模型状态管理器
  late final ModelStateManager modelStateManager;

  // Prompt相关属性
  final TextEditingController promptController = TextEditingController();

  var isFullScreen = false.obs;

  var currentThoughtProcessId = "";
  var currentSubMessageList = <ChatMessageModel>[].obs;
  var showThoughtProcessDetail = false.obs;

  ModelData? currentModel;

  bool isAgentChangeWithoutSave = false;
  bool isLogin = false;

  @override
  Future<void> onInit() async {
    super.onInit();
    chatService = ChatService(this);
    agentConfigService = AgentConfigService(this);
    toolStateManager = ToolStateManager();
    libraryStateManager = LibraryStateManager();
    agentStateManager = AgentStateManager();
    modelStateManager = ModelStateManager();
    initWindow();
    var param = Get.arguments;
    if (param is! AgentModel) {
      Log.e("AgentModel is null");
      Get.back();
      return;
    }
    String agentId = param.id;
    await agentConfigService.initData(agentId);
    // 在初始化数据完成后再添加监听器，避免初始化时触发 isAgentChangeWithoutSave
    promptController.addListener(() => isAgentChangeWithoutSave = true);
    initController();
  }

  void initWindow() async {
    checkIsFullScreen();
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  void initController() {
    // 聊天服务已经初始化了控制器
  }

  // 代理方法 - 数据服务处理
  void initData(String agentId) async {
    await agentConfigService.initData(agentId);
  }

  @override
  Future<void> onClose() async {
    promptController.dispose();
    chatService.dispose();

    EasyLoading.dismiss();

    windowManager.removeListener(this);
    super.onClose();
  }

  @override
  void onWindowResized() async {
    checkIsFullScreen();
    super.onWindowResized();
  }

  @override
  void onWindowClose() {}

  void checkIsFullScreen() async {
    isFullScreen.value = await windowManager.isFullScreen();
  }

  void showMessageThoughtDetail(ChatMessageModel? message) {
    if (message == null) {
      showThoughtProcessDetail.value = false;
      currentSubMessageList.clear();
      currentThoughtProcessId = "";
    } else {
      currentThoughtProcessId = message.taskId ?? "";
      currentSubMessageList.assignAll(message.subMessages ?? []);
      showThoughtProcessDetail.value = true;
    }
  }

  // 代理方法 - 数据服务处理
  void selectModel(String modelId, bool isInit) {
    agentConfigService.selectModel(modelId, isInit);
  }

  // 代理方法 - 数据服务处理
  Future<String> handleCreateModel(ModelFormData modelData) async {
    return await agentConfigService.handleCreateModel(modelData);
  }

  // 状态管理方法
  void setAgentType(String? value) {
    if (value == "0" || value == "GENERAL") {
      agentStateManager.updateAgentType(AgentValidator.DTO_TYPE_GENERAL);
    } else if (value == "1" || value == "DISTRIBUTE") {
      agentStateManager.updateAgentType(AgentValidator.DTO_TYPE_DISTRIBUTE);
    } else if (value == "2" || value == "REFLECTION") {
      if (agentStateManager.childAgentList.isNotEmpty) {
        AlarmUtil.showAlertToast("已添加子Agent，无法改为反思类型");
        return;
      }
      agentStateManager.updateAgentType(AgentValidator.DTO_TYPE_REFLECTION);
    }
    isAgentChangeWithoutSave = true;
  }

  // 代理方法 - 聊天服务处理
  void clearAllMessage() async {
    chatService.clearAllMessage();
  }

  Future<void> handleEditAgent(String name, String iconPath, String description) async {
    var targetAgent = agent.value;
    if (targetAgent != null) {
      targetAgent.name = name;
      targetAgent.iconPath = iconPath;
      targetAgent.description = description;
      chatService.listViewController.agentAvatarPath = iconPath;
      agent.refresh();
      await agentRepository.updateAgent(targetAgent.id, targetAgent);
      eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: targetAgent));
    }
  }

  // 代理方法 - 数据服务处理
  void updateAgentInfo({showToast = true}) {
    agentConfigService.updateAgentInfo(showToast: showToast);
  }

  void showFailToast() {
    AlarmUtil.showAlertToast("Agent初始化失败,请正确配置");
  }

  // 页面跳转方法
  void backToModelPage() async {
    eventBus.fire(MessageEvent(message: EventBusMessage.switchPage, data: HomePageLogic.PAGE_MODEL));
    Get.back();
  }

  void backToToolPage() async {
    eventBus.fire(MessageEvent(message: EventBusMessage.switchPage, data: HomePageLogic.PAGE_TOOL));
    Get.back();
  }

  void backToChat() async {
    String modelId = agent.value?.modelId ?? "";
    var model = await modelRepository.getModelFromBox(modelId);
    if (model == null) {
      AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
      return;
    }
    if (agent.value?.agentType == AgentValidator.DTO_TYPE_REFLECTION) {
      AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
      return;
    }
    eventBus.fire(AgentMessageEvent(message: EventBusMessage.startChat, agent: agent.value));
    Get.back();
  }

  // Agent管理方法
  Future<void> confirmRemoveAgent(String id) async {
    await agentRepository.removeAgent(id);
    eventBus.fire(AgentMessageEvent(message: EventBusMessage.delete, agent: AgentModel.onlyId(id)));
    Get.back();
  }

  // 文件操作方法
  Future<void> exportCurrentAgent(bool exportPlaintext) async {
    var target = agent.value;
    if (target == null) {
      AlarmUtil.showAlertToast("未找到Agent");
      return;
    }
    try {
      String? savePath = await target.exportZip(exportPlaintext);
      AlarmUtil.showAlertToast((savePath?.isNotEmpty ?? false) ? "导出成功" : "导出失败");
    } catch (e) {
      AlarmUtil.showAlertToast("导出失败");
    }
  }
}
