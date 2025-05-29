import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_styled_toast/flutter_styled_toast.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/account.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/uitl/snowflake_uitl.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/server/local_server/agent_server.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_common_confirm.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_child_agent.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_library.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:opentool_dart/opentool_dart.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/local_data_model.dart';
import '../../repositories/library_repository.dart';
import '../../server/local_server/parser.dart';
import '../../utils/alarm_util.dart';
import '../../widgets/dialog/dialog_empty_library.dart';
import '../../widgets/dialog/dialog_select_tool_function.dart';

class AdjustmentLogic extends GetxController with WindowListener {
  final tipsController = TextEditingController();
  final chatScrollController = ScrollController();
  final TextEditingController chatController = TextEditingController();
  final FocusNode chatFocusNode = FocusNode();

  Rx<AgentBean?> agent = Rx<AgentBean?>(null);
  var functionList = <AgentToolFunction>[].obs;

  var modelList = <ModelBean>[].obs;
  var libraryList = <LibraryDto>[].obs;
  var childAgentIdList = <String>[];
  var chatMessageList = <ChatMessage>[].obs;
  var isFullScreen = false.obs;

  var sliderTempValue = 0.0.obs;
  var sliderTokenValue = 4096.0.obs;
  var sliderTopPValue = 1.0.obs;

  var toolHoverItemId = "".obs;
  var libraryHoverItemId = "".obs;
  var messageHoverItemId = "".obs;

  var inputPromptHeight = 120.0.obs;
  var minInputPromptHHeight = 120.0;
  var maxInputPromptHHeight = 300.0;

  var showMoreTool = false.obs;
  var showMoreLibrary = false.obs;
  var enableInput = true.obs;

  var agentType = AgentType.GENERAL.obs;
  var operationMode = OperationMode.PARALLEL.obs;
  var toolOperationMode = OperationMode.PARALLEL.obs;

  AgentLocalServer? agentServer;
  CapabilityDto? lastCapabilityDto;

  var firstConnect = true;
  ModelBean? currentModel;

  AccountDTO? account;
  bool isLogin = false;

  List<ChatMessage> receivingMessageList = [];

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
    isLogin = await accountRepository.isLogin();
    if (isLogin) {
      account = await accountRepository.getAccountInfoFromBox();
    }
    var agent = await agentRepository.getAgentFromBox(agentId);
    this.agent.value = agent;
    if (agent != null) {
      modelList.assignAll((await modelRepository.getModelListFromBox()));
      if (agent.modelId.isNotEmpty) {
        selectModel(agent.modelId);
      }
      tipsController.text = agent.prompt;

      if (agent.functionList != null) {
        functionList.assignAll(agent.functionList!);
      }

      if (isLogin) {
        if (agent.libraryIds != null) {
          for (var id in agent.libraryIds!) {
            var library = await libraryRepository.getLibraryById(id);
            if (library != null) {
              libraryList.add(library);
            }
          }
        }
        libraryList.refresh();
      }

      childAgentIdList.assignAll(agent.childAgentIds ?? []);

      sliderTempValue.value = agent.temperature.toDouble();
      sliderTokenValue.value = agent.maxToken.toDouble();
      sliderTopPValue.value = agent.topP;

      agentType.value = agent.agentType ?? AgentType.GENERAL;
      enableInput.value = agent.agentType != AgentType.REFLECTION;
      operationMode.value = agent.operationMode ?? OperationMode.PARALLEL;
      toolOperationMode.value = agent.toolOperationMode ?? OperationMode.PARALLEL;

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
        currentModel = model;
        String maxTokenString = model.maxToken ?? "4096";
        int maxToken = int.parse(maxTokenString);
        if (sliderTokenValue.value > maxToken) {
          sliderTokenValue.value = maxToken.toDouble();
        }
        modelList.refresh();
        break;
      }
    }
  }

  void removeFunction(int index) {
    functionList.removeAt(index);
  }

  void showToolFunctionSelectDialog() {
    Get.dialog(
      barrierDismissible: false,
      SelectToolFunctionDialog(selectToolFunctionList: functionList),
    );
  }

  void removeLibrary(int index) {
    libraryList.removeAt(index);
  }

  Future<void> showLibrarySelectDialog() async {
    if (!isLogin) {
      AlarmUtil.showAlertToast("未登录无法选择线上知识库");
      return;
    }
    if (await libraryRepository.checkIsLibraryListEmpty()) {
      showEmptyLibraryDialog();
      return;
    }
    var list = libraryList.map((library) => library.id).toList();
    Get.dialog(
      barrierDismissible: false,
      SelectLibraryDialog(
          selectLibraryId: list,
          onConfirm: (target) {
            for (var library in libraryList) {
              if (target.id == library.id) {
                libraryList.remove(library);
                return;
              }
            }
            libraryList.add(target);
          }),
    );
  }

  void showChildAgentSelectDialog() {
    if (agentType.value == AgentType.REFLECTION) {
      AlarmUtil.showAlertToast("反思类型不能添加子子Agent");
      return;
    }
    Get.dialog(
      barrierDismissible: false,
      SelectChildAgentDialog(selectAgentId: childAgentIdList, currentAgentId: agent.value?.id ?? ""),
    );
  }

  void showCreateModelDialog() {
    Get.dialog(
        barrierDismissible: false,
        EditModelDialog(
            model: null,
            isEdit: false,
            onConfirmCallback: (String name, String baseUrl, String apiKey, int maxToken) {
              ModelBean model = ModelBean();
              model.id = snowFlakeUtil.getId();
              modelList.add(model);
              model.name = name;
              model.url = baseUrl;
              model.key = apiKey;
              model.maxToken = maxToken.toString();
              model.createTime = DateTime.now().microsecondsSinceEpoch;
              //modelList.refresh();
              selectModel(model.id);
              modelRepository.updateModel(model.id, model);
              eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateList));
            }));
  }

  void setAgentType(String? value) {
    enableInput.value = true;
    if (value == "GENERAL") {
      agentType.value = AgentType.GENERAL;
    } else if (value == "DISTRIBUTE") {
      agentType.value = AgentType.DISTRIBUTE;
    } else if (value == "REFLECTION") {
      if (childAgentIdList.isNotEmpty) {
        AlarmUtil.showAlertToast("已添加子Agent，无法改为反思类型");
        return;
      }
      agentType.value = AgentType.REFLECTION;
      chatController.text = "";
      enableInput.value = false;
    }
  }

  void sendMessage(String message) {
    if (!(agentServer?.isConnecting() ?? false)) {
      showFailToast();
      return;
    }
    if (agent.value?.agentType == AgentType.REFLECTION) {
      AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
      return;
    }
    if (message.trim().isEmpty) {
      return;
    }

    //create User message model
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
              onConfirmCallback: (name, iconPath, description) async {
                targetAgent.name = name;
                targetAgent.iconPath = iconPath;
                targetAgent.description = description;
                agent.refresh();
                await agentRepository.updateAgent(targetAgent.id, targetAgent);
                eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: targetAgent));
              }));
    }
  }

  Future<void> startChat(bool updateInfo, bool showToast) async {
    receivingMessageList.clear();
    await agentServer?.clearChat();

    agentServer = AgentLocalServer();

    CapabilityDto? capabilityDto;
    //After clear the chat message, avoid duplicate create capabilityDto
    if (!updateInfo && lastCapabilityDto != null) {
      capabilityDto = lastCapabilityDto!;
    } else {
      var agent = this.agent.value;
      if (agent != null) {
        capabilityDto = await agentServer?.buildLocalAgentCapabilityDto(agent);
      }
    }

    if (capabilityDto != null) {
      await agentServer?.initChat(capabilityDto, (agentMessage) {
        parseAgentMessage(agentMessage);
        try {
          if (agentMessage.role == ToolRoleType.USER && agentMessage.to == ToolRoleType.AGENT) {
            //create Agent message model
            var receivedMessage = ChatMessage();
            receivedMessage.sendRole = ChatRole.Agent;
            receivedMessage.taskId = agentMessage.taskId;
            receivedMessage.isLoading = true;
            chatMessageList.add(receivedMessage);
            receivingMessageList.add(receivedMessage);
          } else if (agentMessage.role == ToolRoleType.AGENT && agentMessage.to == ToolRoleType.CLIENT) {
            if (agentMessage.type == AgentMessageType.TASK_STATUS) {
              //TaskStatus status = TaskStatus.fromJson(agentMessage.content);
              Map<String, dynamic> json = agentMessage.content;
              if (json["status"] == "done") {
                var message = getTargetMessage(agentMessage.taskId);
                message?.isLoading = false;
                receivingMessageList.remove(message);
              } else if (json["status"] == "stop") {
                var message = getTargetMessage(agentMessage.taskId);
                message?.isLoading = false;
                message?.message = "服务暂停,请再试";
                receivingMessageList.remove(message);
              } else if (json["status"] == "exception") {
                var message = getTargetMessage(agentMessage.taskId);
                message?.isLoading = false;
                message?.message = jsonEncode(json["description"]);
                receivingMessageList.remove(message);
              }
            }
          } else if (agentMessage.role == ToolRoleType.AGENT && agentMessage.to == ToolRoleType.TOOL) {
            if (agentMessage.type == ToolMessageType.FUNCTION_CALL_LIST) {
              List<dynamic> originalFunctionCallList = agentMessage.content as List<dynamic>;
              List<FunctionCall> functionCallList = originalFunctionCallList.map((dynamic json) => FunctionCall.fromJson(json)).toList();
              for (var functionCall in functionCallList) {
                if (!functionCall.name.isNumericOnly) {
                  Thought thought = Thought();
                  thought.type = ThoughtRoleType.Tool;
                  thought.id = functionCall.id;
                  thought.roleName = functionCall.name;
                  thought.sentMessage = jsonEncode(functionCall.parameters);
                  var message = getTargetMessage(agentMessage.taskId);
                  if (message != null) {
                    message.thoughtList ??= [];
                    message.thoughtList?.add(thought);
                  }
                }
              }
            }
          } else if (agentMessage.role == ToolRoleType.TOOL && agentMessage.to == ToolRoleType.AGENT) {
            if (agentMessage.type == ToolMessageType.TOOL_RETURN) {
              ToolReturn toolReturn = ToolReturn.fromJson(agentMessage.content);
              var message = getTargetMessage(agentMessage.taskId);
              var list = message?.thoughtList;
              bool isTool = false;
              if (list != null) {
                for (var thought in list) {
                  if (thought.id == toolReturn.id) {
                    thought.receivedMessage = jsonEncode(toolReturn.result);
                    isTool = true;
                    break;
                  }
                }
              }
              if (!isTool) {
                String result = toolReturn.result["result"];
                message?.childAgentMessageList ??= [];
                message?.childAgentMessageList?.add(result);
              }
            }
          } else if (agentMessage.role == ToolRoleType.AGENT && agentMessage.to == ToolRoleType.USER) {
            if (agentMessage.type == ToolMessageType.TEXT) {
              var message = getTargetMessage(agentMessage.taskId);
              message?.message = agentMessage.content as String;
            }
          }
          chatMessageList.refresh();
          scrollListToBottom(false);
        } catch (e) {
          print(e.toString());
        }
      });
    }

    if (agentServer?.isConnecting() ?? false) {
      lastCapabilityDto = capabilityDto;
    } else {
      if (showToast) {
        showFailToast();
      }
    }
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

  ChatMessage? getTargetMessage(String taskId) {
    for (var message in receivingMessageList) {
      if (message.taskId == taskId) {
        return message;
      }
    }
    return null;
  }

  void updateAgentInfo() async {
    try {
      var agent = this.agent.value;
      if (agent != null && agent.id.isNotEmpty) {
        agent.modelId = currentModel?.id ?? "";
        agent.prompt = tipsController.text;
        agent.temperature = (sliderTempValue.value * 10).round() / 10;
        agent.maxToken = sliderTokenValue.value.toInt();
        agent.topP = (sliderTopPValue.value * 10).round() / 10;
        agent.functionList ??= [];
        agent.functionList?.assignAll(functionList);
        if (isLogin) {
          agent.libraryIds = libraryList.map((library) => library.id).toList();
        }
        agent.agentType = agentType.value;
        agent.operationMode = operationMode.value;
        agent.toolOperationMode = toolOperationMode.value;
        agent.childAgentIds ??= [];
        agent.childAgentIds?.assignAll(childAgentIdList);

        await agentRepository.updateAgent(agent.id, agent);
        eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: agent));
      }
      showToast("保存成功",
          animation: StyledToastAnimation.fade,
          reverseAnimation: StyledToastAnimation.fade,
          position: StyledToastPosition.center,
          context: Get.context);
    } catch (e) {
      print(e);
      showToast("保存失败",
          animation: StyledToastAnimation.fade,
          reverseAnimation: StyledToastAnimation.fade,
          position: StyledToastPosition.center,
          context: Get.context);
    }
    await startChat(true, true);
  }

  void showFailToast() {
    showToast("Agent初始化失败,请正确配置",
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
    if (agent.value?.agentType == AgentType.REFLECTION) {
      AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
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
          content: "即将删除Agent的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () async {
            await agentRepository.removeAgent(id);
            eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateList));
            Get.back();
          },
        ));
  }

  void copyToClipboard(String string) {
    Clipboard.setData(ClipboardData(text: string)).then((text) => AlarmUtil.showAlertToast("复制成功"));
  }

  void showEmptyLibraryDialog() {
    Get.dialog(barrierDismissible: false, EmptyLibraryDialog());
  }

  void updateHeight(double deltaY) {
    inputPromptHeight.value = (inputPromptHeight.value + deltaY).clamp(minInputPromptHHeight, maxInputPromptHHeight);
  }
}
