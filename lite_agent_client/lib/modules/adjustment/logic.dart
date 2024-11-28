import 'package:flutter/material.dart';
import 'package:flutter_styled_toast/flutter_styled_toast.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/server/local_server/agent_server.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_common_confirm.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_tool.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_tool_edit.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/local_data_model.dart';
import '../../utils/alarm_util.dart';

class AdjustmentLogic extends GetxController with WindowListener {
  final tipsController = TextEditingController();
  final chatScrollController = ScrollController();
  final TextEditingController chatController = TextEditingController();
  final FocusNode chatFocusNode = FocusNode();

  Rx<AgentBean?> agent = Rx<AgentBean?>(null);
  var toolList = <ToolBean>[].obs;
  var modelList = <ModelBean>[].obs;
  var chatMessageList = <ChatMessage>[].obs;
  var isFullScreen = false.obs;
  var selectedModelId = "";

  var sliderTempValue = 0.0.obs;
  var sliderTokenValue = 4096.0.obs;
  var sliderTopPValue = 1.0.obs;

  var toolHoverItemId = "".obs;

  AgentLocalServer? agentServer;
  CapabilityDto? lastCapabilityDto;

  var firstConnect = true;

  @override
  void onInit() {
    super.onInit();
    var param = Get.arguments;
    if (param is! AgentBean) {
      Get.back();
      return;
    }
    String agentId = param.id;
    initData(agentId);
    initWindow();
  }

  void initWindow() async {
    checkIsFullScreen();
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  void initData(String agentId) async {
    var agent = await agentRepository.getAgentFromBox(agentId);
    this.agent.value = agent;
    if (agent != null) {
      modelList.assignAll((await modelRepository.getModelListFromBox()));
      if (agent.modelId.isNotEmpty) {
        selectModel(agent.modelId);
      }
      tipsController.text = agent.prompt;

      for (var toolId in agent.toolList) {
        var tool = await toolRepository.getToolFromBox(toolId);
        if (tool != null) {
          toolList.add(tool);
        }
      }

      sliderTempValue.value = agent.temperature.toDouble();
      sliderTokenValue.value = agent.maxToken.toDouble();
      sliderTopPValue.value = agent.topP;
      await startChat(false, false);
    }
  }

  @override
  void onClose() {
    tipsController.dispose();
    chatFocusNode.dispose();
    chatController.dispose();

    agentServer?.clearChat();

    windowManager.removeListener(this);
    super.onClose();
  }

  @override
  void onWindowResized() async {
    checkIsFullScreen();
    super.onWindowResized();
  }

  @override
  void onWindowClose() {
    Get.back();
  }

  void goBack() {
    Get.back();
  }

  void checkIsFullScreen() async {
    isFullScreen.value = await windowManager.isFullScreen();
  }

  void selectModel(String modelId) {
    for (var model in modelList) {
      if (model.id == modelId) {
        selectedModelId = modelId;
        modelList.refresh();
        break;
      }
    }
  }

  void removeTool(String id) {
    for (var tool in toolList) {
      if (tool.id == id) {
        toolList.remove(tool);
        break;
      }
    }
  }

  void updateTool(String id, String name, String description, String schemaType, String schemaText, String apiType, String apiText) {
    ToolBean? targetTool;
    if (id.isNotEmpty) {
      for (var tool in toolList) {
        if (tool.id == id) {
          targetTool = tool;
          break;
        }
      }
    } else {
      targetTool = ToolBean();
      targetTool.id = DateTime.now().microsecondsSinceEpoch.toString();
      toolList.add(targetTool);
    }
    if (targetTool != null) {
      targetTool.name = name;
      targetTool.description = description;
      targetTool.schemaText = schemaText;
      targetTool.schemaType = schemaType;
      targetTool.apiText = apiText;
      targetTool.apiType = apiType;
      toolList.refresh();
      toolRepository.updateTool(targetTool.id, targetTool);
      eventBus.fire(ToolMessageEvent(message: EventBusMessage.updateList));
    }
  }

  void showCreateToolDialog() {
    showEditToolDialog(null);
  }

  void showEditToolDialog(ToolBean? tool) {
    Get.dialog(
        barrierDismissible: false,
        EditToolDialog(
            tool: tool,
            isEdit: tool != null,
            onConfirmCallback: (String name, String description, String schemaType, String schemaText, String apiType, String apiText) {
              updateTool(tool?.id ?? "", name, description, schemaType, schemaText, apiType, apiText);
            }));
  }

  void showToolSelectDialog() {
    var list = <String>[];
    for (var tool in toolList) {
      list.add(tool.id);
    }
    Get.dialog(
        barrierDismissible: false,
        SelectToolDialog(
          selectIdList: list,
          onConfirm: (List<ToolBean> tools) {
            toolList.assignAll(tools);
          },
        ));
  }

  void showCreateModelDialog() {
    Get.dialog(
        barrierDismissible: false,
        EditModelDialog(
            model: null,
            isEdit: false,
            onConfirmCallback: (String name, String baseUrl, String apiKey) {
              ModelBean model = ModelBean();
              model.id = DateTime.now().microsecondsSinceEpoch.toString();
              modelList.add(model);
              model.name = name;
              model.url = baseUrl;
              model.key = apiKey;
              //modelList.refresh();
              selectModel(model.id);
              modelRepository.updateModel(model.id, model);
              eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateList));
            }));
  }

  void sendMessage(String message) {
    if (!(agentServer?.isConnecting() ?? false)) {
      showFailToast();
      return;
    }
    if (message.trim().isEmpty) {
      return;
    }
    var chatMessage = ChatMessage();
    chatMessage.message = message;
    chatMessage.userName = "userName";
    chatMessage.sendRole = ChatRole.User;
    chatMessageList.add(chatMessage);
    agentServer?.sendUserMessage(message);
    scrollListToBottom(true);
  }

  void clearAllMessage() async {
    chatMessageList.clear();
    startChat(false, true);
  }

  onSendButtonPress() {
    chatFocusNode.requestFocus();
    var message = chatController.text;
    if (message.isNotEmpty) {
      sendMessage(message);
      chatController.text = "";
    }
  }

  void showEditAgentDialog() {
    var targetAgent = agent.value;
    if (targetAgent != null) {
      Get.dialog(
          barrierDismissible: false,
          EditAgentDialog(
              name: targetAgent.name,
              iconPath: targetAgent.iconPath,
              description: targetAgent.description,
              isEdit: targetAgent.id.isNotEmpty,
              onConfirmCallback: (name, iconPath, description) {
                targetAgent.name = name;
                targetAgent.iconPath = iconPath;
                targetAgent.description = description;
                agent.refresh();
                eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: targetAgent));
              }));
    }
  }

  Future<void> startChat(bool updateInfo, bool showToast) async {
    //await agentServer?.stopChat();
    //if (!updateInfo) {
    await agentServer?.clearChat();
    //}
    agentServer = AgentLocalServer();

    late CapabilityDto capabilityDto;
    if (!updateInfo && lastCapabilityDto != null) {
      capabilityDto = lastCapabilityDto!;
    } else {
      var model = await modelRepository.getModelFromBox(selectedModelId);
      if (model == null) {
        if (showToast) {
          showFailToast();
        }
        return;
      }
      capabilityDto = createCapabilityDto(model);
    }

    await agentServer?.initChat(capabilityDto);
    await agentServer?.connectChat((message) {
      chatMessageList.add(message);
      scrollListToBottom(false);
    });

    if (agentServer?.isConnecting() ?? false) {
      lastCapabilityDto = capabilityDto;
    } else {
      if (showToast) {
        showFailToast();
      }
    }
  }

  CapabilityDto createCapabilityDto(ModelBean model) {
    var temperature = (sliderTempValue.value * 10).round() / 10;
    var maxToken = sliderTokenValue.value.toInt();
    var topP = (sliderTopPValue.value * 10).round() / 10;
    LLMConfigDto llmConfig =
        LLMConfigDto(baseUrl: model.url, apiKey: model.key, model: model.name, temperature: temperature, maxTokens: maxToken, topP: topP);

    String prompt = tipsController.text;

    List<OpenSpecDto> openSpecList = [];
    for (var tool in toolList) {
      ApiKeyDto? apiKey;
      if (tool.apiType == "basic" || tool.apiType == "Basic") {
        apiKey = ApiKeyDto(type: ApiKeyType.basic, apiKey: tool.apiText);
      } else if (tool.apiType == "bearer" || tool.apiType == "Bearer") {
        apiKey = ApiKeyDto(type: ApiKeyType.bearer, apiKey: tool.apiText);
      }
      String protocol = "";
      if (tool.schemaType == SchemaType.openapi || tool.schemaType == Protocol.openapi) {
        protocol = Protocol.openapi;
      } else if (tool.schemaType == SchemaType.jsonrpcHttp || tool.schemaType == Protocol.jsonrpcHttp) {
        protocol = Protocol.jsonrpcHttp;
      } else if (tool.schemaType == SchemaType.openmodbus || tool.schemaType == Protocol.openmodbus) {
        protocol = Protocol.openmodbus;
      }
      if (protocol.isEmpty) {
        continue;
      }
      OpenSpecDto openSpec = OpenSpecDto(openSpec: tool.schemaText, protocol: tool.schemaType, apiKey: apiKey);
      openSpecList.add(openSpec);
    }

    return CapabilityDto(llmConfig: llmConfig, systemPrompt: prompt, openSpecList: openSpecList, timeoutSeconds: 20);
  }

  void scrollListToBottom(bool needAnim) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (needAnim) {
        chatScrollController.animateTo(
          chatScrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOutQuart,
        );
      } else {
        chatScrollController.jumpTo(chatScrollController.position.maxScrollExtent);
      }
    });
  }

  void updateAgentInfo() async {
    try {
      var agent = this.agent.value;
      if (agent != null && agent.id.isNotEmpty) {
        agent.modelId = selectedModelId;
        agent.prompt = tipsController.text;
        agent.toolList.clear();
        for (var tool in toolList) {
          agent.toolList.add(tool.id);
        }
        agent.temperature = (sliderTempValue.value * 10).round() / 10;
        agent.maxToken = sliderTokenValue.value.toInt();
        agent.topP = (sliderTopPValue.value * 10).round() / 10;
        await agentRepository.updateAgent(agent.id, agent);
        eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: agent));
      }
      showToast("保存成功",
          animation: StyledToastAnimation.fade,
          reverseAnimation: StyledToastAnimation.fade,
          position: StyledToastPosition.center,
          context: Get.context);
    } catch (e) {
      showToast("保存失败$e",
          animation: StyledToastAnimation.fade,
          reverseAnimation: StyledToastAnimation.fade,
          position: StyledToastPosition.center,
          context: Get.context);
    }
    await startChat(true, true);
  }

  void showFailToast() {
    showToast("ai模型初始化失败,请正确配置模型",
        animation: StyledToastAnimation.fade,
        reverseAnimation: StyledToastAnimation.fade,
        position: StyledToastPosition.center,
        context: Get.context);
  }

  void backToChat() async {
    String modelId = agent.value?.modelId ?? "";
    var model = await modelRepository.getModelFromBox(modelId);
    if (model == null) {
      AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
      return;
    }
    eventBus.fire(AgentMessageEvent(message: EventBusMessage.startChat, agent: agent.value));
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
            await agentRepository.removeAgent(id);
            eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateList));
            Get.back();
          },
        ));
  }
}
