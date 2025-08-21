import 'dart:async';
import 'dart:io';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/uitl/snowflake_uitl.dart';
import 'package:lite_agent_client/modules/home/logic.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/server/local_server/agent_server.dart';
import 'package:lite_agent_client/server/local_server/audio_service.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_common_confirm.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_child_agent.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_library.dart';
import 'package:lite_agent_client/widgets/listview_chat_message.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:path_provider/path_provider.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/local_data_model.dart';
import '../../repositories/conversation_repository.dart';
import '../../repositories/library_repository.dart';
import '../../server/local_server/parser.dart';
import '../../utils/alarm_util.dart';
import '../../utils/log_util.dart';
import '../../widgets/dialog/dialog_empty_library.dart';
import '../../widgets/dialog/dialog_markdown.dart';
import '../../widgets/dialog/dialog_select_tool_function.dart';
import '../../widgets/input_box_container.dart';

class AdjustmentLogic extends GetxController with WindowListener {
  final tipsController = TextEditingController();

  final Rx<AgentConversationBean> conversation = Rx<AgentConversationBean>(AgentConversationBean());
  Rx<AgentBean?> agent = Rx<AgentBean?>(null);
  var functionList = <AgentToolFunction>[].obs;

  var modelList = <ModelBean>[].obs;
  var ttsModelList = <ModelBean>[];
  var asrModelList = <ModelBean>[];
  var libraryList = <LibraryDto>[].obs;
  var autoAgentModelList = <ModelBean>[].obs;
  var childAgentList = <AgentBean>[].obs;
  var isFullScreen = false.obs;

  var sliderTempValue = 0.0.obs;
  var sliderTokenValue = 4096.0.obs;
  var sliderTopPValue = 1.0.obs;

  var toolHoverItemId = "".obs;
  var libraryHoverItemId = "".obs;
  var messageHoverItemId = "".obs;
  var agentHoverItemId = "".obs;
  var modelHoverItemId = "".obs;

  var inputPromptHeight = 120.0.obs;
  var minInputPromptHHeight = 120.0;
  var maxInputPromptHHeight = 300.0;

  var showMoreTool = false.obs;
  var showMoreLibrary = false.obs;
  var showMoreModel = false.obs;

  var isToolExpanded = false.obs;
  var isLibraryExpanded = false.obs;
  var isAudioExpanded = false.obs;
  var isExecutionModeExpanded = false.obs;
  var isChildAgentsExpanded = false.obs;
  var isModelExpanded = false.obs;

  var enableTextToSpeech = false.obs;
  var enableSpeechToText = false.obs;
  var enableAutoAgent = false.obs;

  var currentThoughtProcessId = "";
  var currentSubMessageList = <ChatMessage>[].obs;
  var showThoughtProcessDetail = false.obs;

  var agentType = AgentType.GENERAL.obs;
  var operationMode = OperationMode.PARALLEL.obs;
  var toolOperationMode = OperationMode.PARALLEL.obs;

  final AudioPlayer _audioPlayer = AudioPlayer();
  StreamSubscription? completeSub;
  StreamSubscription? stateSub;

  final currentTTSModelId = "".obs;
  var currentASRModelId = "".obs;
  final inputBoxController = InputBoxController();
  final ChatMessageListViewController listViewController = ChatMessageListViewController();

  AgentLocalServer? currentAgentServer;
  CapabilityDto? lastCapabilityDto;

  late final MessageHandler _messageHandler;

  ModelBean? currentModel;

  bool isAgentChangeWithoutSave = false;
  bool isLogin = false;

  // Serialize startChat calls to prevent concurrent init
  Completer<bool>? _startChatCompleter;

  Future<bool> _ensureChatStarted() async {
    if (currentAgentServer?.isConnecting() ?? false) return true;
    if (_startChatCompleter != null) return _startChatCompleter!.future;

    final completer = Completer<bool>();
    _startChatCompleter = completer;

    () async {
      bool success = false;
      try {
        success = await startChat();
      } catch (_) {
        success = false;
      } finally {
        if (!completer.isCompleted) completer.complete(success);
        _startChatCompleter = null;
      }
    }();

    return completer.future;
  }

  @override
  Future<void> onInit() async {
    super.onInit();
    initWindow();
    var param = Get.arguments;
    if (param is! AgentBean) {
      Get.back();
      return;
    }
    String agentId = param.id;
    initData(agentId);
    initController();
  }

  void initWindow() async {
    checkIsFullScreen();
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  void initController() {
    inputBoxController
      ..onSendButtonPress = sendMessage
      ..onAudioRecordFinish = onAudioRecordFinish;

    listViewController
      ..onMessageThoughButtonClick = showMessageThoughtDetail
      ..onMessageAudioButtonClick = playMessageAudio;

    _messageHandler = MessageHandler(
      chatMessageList: conversation.value.chatMessageList,
      onThoughtUpdate: (message) {
        if (showThoughtProcessDetail.value && message.taskId == currentThoughtProcessId) {
          showMessageThoughtDetail(message);
        }
      },
      onAgentReply: (message) {
        if (currentTTSModelId.value.isNotEmpty) {
          int index = conversation.value.chatMessageList.indexOf(message);
          playMessageAudio(index, message.message);
        }
      },
      onHandlerFinished: () {
        conversation.refresh();
        listViewController.scrollListToBottom();
        conversationRepository.updateAdjustmentHistory(conversation.value.agentId, conversation.value);
      },
    );
  }

  void initData(String agentId) async {
    var history = await conversationRepository.getAdjustmentHistory(agentId);
    conversation.update((conversation) {
      conversation?.agentId = agentId;
      conversation?.chatMessageList.assignAll(history?.chatMessageList ?? []);
    });

    if (conversation.value.chatMessageList.isNotEmpty) {
      conversation.refresh();
      Future.delayed(const Duration(milliseconds: 100), () {
        listViewController.scrollListToBottom(animate: false);
        // 再次延迟确保滚动完成
        Future.delayed(const Duration(milliseconds: 100), () => listViewController.scrollListToBottom(animate: false));
      });
    }

    isLogin = await accountRepository.isLogin();
    if (isLogin) {
      var account = await accountRepository.getAccountInfoFromBox();
      listViewController.agentAvatarPath = account?.avatar ?? "";
    }
    var agent = await agentRepository.getAgentFromBox(agentId);
    this.agent.value = agent;
    if (agent != null) {
      var isAutoAgent = agent.autoAgentFlag ?? false;
      listViewController.agentAvatarPath = agent.iconPath;

      var allModels = await modelRepository.getModelListFromBox();

      sliderTempValue.value = agent.temperature.toDouble();
      sliderTokenValue.value = agent.maxToken.toDouble();
      sliderTopPValue.value = agent.topP;

      modelList.assignAll(allModels.where((model) => model.type == "LLM" || model.type == null));
      if (isAutoAgent) {
        autoAgentModelList.assignAll(modelList.where((model) => model.supportMultiAgent == true));
      }
      ttsModelList = allModels.where((model) => model.type == "TTS").toList();
      asrModelList = allModels.where((model) => model.type == "ASR").toList();

      if (agent.modelId.isNotEmpty) {
        selectModel(agent.modelId, true);
      }
      tipsController.text = agent.prompt;
      tipsController.addListener(() => isAgentChangeWithoutSave = true);

      if (isAutoAgent) {
        var toolList = await toolRepository.getToolListFromBox();
        var autoAgentToolList = toolList.where((tool) => tool.supportMultiAgent == true).toList();
        for (var tool in autoAgentToolList) {
          await tool.initToolFunctionList();
          for (var function in tool.functionList) {
            function.toolName = tool.name;
            functionList.add(function);
          }
        }
      } else {
        if (agent.functionList != null) {
          for (var function in agent.functionList!) {
            var tool = await toolRepository.getToolFromBox(function.toolId);
            if (tool != null) {
              function.toolName = tool.name;
            }
            functionList.add(function);
          }
        }
      }

      if (isLogin && !isAutoAgent) {
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

      if (agent.ttsModelId != null && agent.ttsModelId!.isNotEmpty) {
        currentTTSModelId.value = agent.ttsModelId!;
        enableTextToSpeech.value = true;
        listViewController.setAudioButtonVisible(true);
      }
      if (agent.asrModelId != null && agent.asrModelId!.isNotEmpty) {
        currentASRModelId.value = agent.asrModelId!;
        enableSpeechToText.value = true;
        inputBoxController.setEnableAudioInput(true);
      }

      if (agent.childAgentIds != null) {
        for (var id in agent.childAgentIds!) {
          var childAgent = await agentRepository.getAgentFromBox(id);
          if (childAgent != null) {
            childAgentList.add(childAgent);
          }
        }
      }

      agentType.value = agent.agentType ?? AgentType.GENERAL;

      var enableInput = agent.agentType != AgentType.REFLECTION;
      inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");
      if (!enableInput) {
        inputBoxController.switchInputWay(false);
      }
      operationMode.value = agent.operationMode ?? OperationMode.PARALLEL;
      toolOperationMode.value = agent.toolOperationMode ?? OperationMode.PARALLEL;

      if (isAutoAgent) {
        updateAgentInfo(showToast: false);
        isModelExpanded.value = true;
        isToolExpanded.value = true;
        isAudioExpanded.value = true;
      }
    }
  }

  @override
  Future<void> onClose() async {
    tipsController.dispose();
    _audioPlayer.stop();
    completeSub = null;
    stateSub = null;
    _audioPlayer.dispose();
    inputBoxController.dispose();
    listViewController.dispose();

    lastCapabilityDto = null;
    currentAgentServer?.clearChat();
    currentAgentServer = null;
    _messageHandler.dispose();

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

  void goBack() {
    if (isAgentChangeWithoutSave) {
      Get.dialog(
        barrierDismissible: true,
        CommonConfirmDialog(
          title: "系统信息",
          content: "有内容未保存，确认离开？",
          confirmString: "",
          onConfirmCallback: () async => Get.back(),
        ),
      );
      return;
    }
    Get.back();
  }

  void checkIsFullScreen() async {
    isFullScreen.value = await windowManager.isFullScreen();
  }

  void showMessageThoughtDetail(ChatMessage? message) {
    if (message == null) {
      showThoughtProcessDetail.value = false;
      //currentThoughtList.clear();
      currentSubMessageList.clear();
      currentThoughtProcessId = "";
    } else {
      currentThoughtProcessId = message.taskId ?? "";
      //currentThoughtList.assignAll(message.thoughtList ?? []);
      currentSubMessageList.assignAll(message.subMessages ?? []);
      showThoughtProcessDetail.value = true;
    }
  }

  void selectModel(String modelId, bool isInit) {
    for (var model in modelList) {
      if (model.id == modelId) {
        currentModel = model;
        String maxTokenString = model.maxToken ?? "4096";
        int maxToken = int.parse(maxTokenString);
        if (sliderTokenValue.value > maxToken) {
          sliderTokenValue.value = maxToken.toDouble();
        }
        if (!isInit) {
          isAgentChangeWithoutSave = true;
        }
        modelList.refresh();
        break;
      }
    }
  }

  void removeFunction(int index) {
    functionList.removeAt(index);
    isAgentChangeWithoutSave = true;
  }

  void showToolFunctionSelectDialog() {
    Get.dialog(
      barrierDismissible: false,
      SelectToolFunctionDialog(
        selectToolFunctionList: functionList,
        onSelectChanged: () => isAgentChangeWithoutSave = true,
      ),
    );
  }

  void removeLibrary(int index) {
    libraryList.removeAt(index);
    isAgentChangeWithoutSave = true;
  }

  void removeChildAgent(int index) {
    childAgentList.removeAt(index);
    isAgentChangeWithoutSave = true;
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
                isAgentChangeWithoutSave = true;
                return;
              }
            }
            libraryList.add(target);
            isAgentChangeWithoutSave = true;
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
      SelectChildAgentDialog(
        selectAgents: childAgentList,
        currentAgentId: agent.value?.id ?? "",
        onSelectChanged: () => isAgentChangeWithoutSave = true,
      ),
    );
  }

  void showCreateModelDialog() {
    Get.dialog(
        barrierDismissible: false,
        EditModelDialog(
            model: null,
            isEdit: false,
            onConfirmCallback: (ModelEditParams params) {
              ModelBean model = ModelBean();
              model.id = snowFlakeUtil.getId();
              modelList.add(model);
              model.type = params.type;
              model.name = params.name;
              model.nickName = params.nickName;
              model.url = params.baseUrl;
              model.key = params.apiKey;
              model.maxToken = params.maxToken.toString();
              model.supportMultiAgent = params.supportMultiAgent;
              model.supportToolCalling = params.supportToolCalling;
              model.supportDeepThinking = params.supportDeepThinking;
              model.createTime = DateTime.now().microsecondsSinceEpoch;
              //modelList.refresh();
              selectModel(model.id, false);
              modelRepository.updateModel(model.id, model);
              eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateList));
            }));
  }

  void setAgentType(String? value) {
    if (value == "GENERAL") {
      agentType.value = AgentType.GENERAL;
    } else if (value == "DISTRIBUTE") {
      agentType.value = AgentType.DISTRIBUTE;
    } else if (value == "REFLECTION") {
      if (childAgentList.isNotEmpty) {
        AlarmUtil.showAlertToast("已添加子Agent，无法改为反思类型");
        return;
      }
      agentType.value = AgentType.REFLECTION;
    }
    isAgentChangeWithoutSave = true;
  }

  playMessageAudio(int index, String content) async {
    if (content.isEmpty) {
      AlarmUtil.showAlertToast("结果为空不可转换");
      return;
    }
    final selectedModel = ttsModelList.firstWhereOrNull((model) => model.id == currentTTSModelId.value);
    if (selectedModel == null) {
      AlarmUtil.showAlertToast("请选择TTS模型");
      return;
    }
    try {
      if (_audioPlayer.state == PlayerState.playing) {
        _audioPlayer.stop();
        if (listViewController.activeAudioMessageId == index.toString()) {
          return;
        }
      }

      EasyLoading.show(status: "正在转换...");
      var file = await AudioService.textToSpeech(
          llmConfig: LLMConfig(model: selectedModel.name, baseUrl: selectedModel.url, apiKey: selectedModel.key),
          text: content,
          outputDirectory: await getTemporaryDirectory());

      completeSub = _audioPlayer.onPlayerComplete.listen((event) async {
        if (await file.exists()) {
          await file.delete();
        }
        listViewController.activeAudioMessageId = "";
        listViewController.refreshButton();
        completeSub?.cancel(); // 清理订阅
        stateSub?.cancel();
      });
      stateSub = _audioPlayer.onPlayerStateChanged.listen((state) async {
        if (state == PlayerState.stopped) {
          if (await file.exists()) {
            await file.delete();
          }
          listViewController.activeAudioMessageId = "";
          listViewController.refreshButton();
          completeSub?.cancel(); // 清理订阅
          stateSub?.cancel();
        }
      });
      listViewController.activeAudioMessageId = index.toString();
      listViewController.refreshButton();
      await _audioPlayer.play(DeviceFileSource(file.path));
      EasyLoading.dismiss();
    } on Exception catch (e) {
      listViewController.activeAudioMessageId = "";
      listViewController.refreshButton();
      EasyLoading.dismiss();
      AlarmUtil.showAlertToast("转换出错");
      throw Exception('文本转语音失败: $e');
    }
  }

  Future<void> sendMessage(String message) async {
    if (agent.value?.agentType == AgentType.REFLECTION) {
      AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
      return;
    }

    if (message.trim().isEmpty) {
      return;
    }

    // If startChat is still in progress, block repeated submissions
    if (_startChatCompleter != null && !(currentAgentServer?.isConnecting() ?? false)) {
      AlarmUtil.showAlertToast("Agent未初始化完成，请勿重复提交");
      return;
    }

    // Ensure startChat completes before sending
    if (!(await _ensureChatStarted())) {
      showFailToast();
      return;
    }

    var sendMessage = ChatMessage()
      ..message = message
      ..roleName = "userName"
      ..sendRole = ChatRoleType.User;
    conversation.value.chatMessageList.add(sendMessage);

    listViewController.scrollListToBottom();
    conversationRepository.updateAdjustmentHistory(conversation.value.agentId, conversation.value);

    currentAgentServer?.sendUserMessage(message);
  }

  void clearAllMessage() async {
    showThoughtProcessDetail.value = false;
    conversation.value.chatMessageList.clear();
    conversation.refresh();
    await conversationRepository.updateAdjustmentHistory(conversation.value.agentId, conversation.value);
    await currentAgentServer?.clearChat();
    currentAgentServer = null;
  }

  void showPromptPreviewDialog() async {
    String prompt = tipsController.text;
    Get.dialog(
      barrierDismissible: false,
      MarkDownTextDialog(titleText: "提示词预览", contentText: prompt),
    );
  }

  Future<void> onAudioRecordFinish(File recordFile) async {
    final selectedModel = asrModelList.firstWhereOrNull((model) => model.id == currentASRModelId.value);
    if (selectedModel == null) {
      AlarmUtil.showAlertToast("请选择ASR模型");
      return;
    }

    try {
      inputBoxController.setAudioEnable(false);
      EasyLoading.show(status: "正在转换...");
      String message = await AudioService.speechToText(
        llmConfig: LLMConfig(model: selectedModel.name, baseUrl: selectedModel.url, apiKey: selectedModel.key),
        audioFile: recordFile,
        language: "zh",
      );
      inputBoxController.setAudioEnable(true);
      EasyLoading.dismiss();
      if (message.isNotEmpty) {
        sendMessage(message);
      } else {
        AlarmUtil.showAlertToast("识别不到内容，请重试");
      }
    } on Exception catch (e) {
      inputBoxController.setAudioEnable(true);
      EasyLoading.dismiss();
      AlarmUtil.showAlertToast("转换出错");
      throw Exception('语音转文本失败: $e');
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
                listViewController.agentAvatarPath = iconPath;
                agent.refresh();
                await agentRepository.updateAgent(targetAgent.id, targetAgent);
                eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: targetAgent));
              }));
    }
  }

  Future<bool> startChat() async {
    var agent = this.agent.value;

    await currentAgentServer?.clearChat();

    if (agent?.agentType == AgentType.REFLECTION) {
      return false;
    }

    var agentServer = AgentLocalServer();
    CapabilityDto? capabilityDto;

    if (agent != null) {
      capabilityDto = await agentServer.buildLocalAgentCapabilityDto(agent, autoModelList: autoAgentModelList);
    }
    _messageHandler.subAgentNameMap = agentServer.subAgentNameMap;

    if (capabilityDto != null) {
      await agentServer.initChat(capabilityDto);
    }

    if (agentServer.isConnecting()) {
      agentServer.setMessageHandler(_messageHandler);
      lastCapabilityDto = capabilityDto;
      currentAgentServer = agentServer;
      return true;
    }
    return false;
  }

  void updateAgentInfo({showToast = true}) async {
    showThoughtProcessDetail.value = false;
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
        var enableInput = agent.agentType != AgentType.REFLECTION;
        inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");
        if (!enableInput) {
          inputBoxController.switchInputWay(false);
        }
        agent.operationMode = operationMode.value;
        agent.toolOperationMode = toolOperationMode.value;
        agent.childAgentIds ??= [];
        agent.childAgentIds?.assignAll(childAgentList.map((childAgent) => childAgent.id).toList());

        agent.ttsModelId = currentTTSModelId.value;
        agent.asrModelId = currentASRModelId.value;

        await agentRepository.updateAgent(agent.id, agent);
        eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: agent));
      }
      if (showToast) {
        AlarmUtil.showAlertToast("保存成功");
      }
      isAgentChangeWithoutSave = false;

      currentAgentServer?.clearChat();
      currentAgentServer = null;
    } catch (e) {
      Log.e("updateAgentInfo error: $e");
      if (showToast) {
        AlarmUtil.showAlertToast("保存失败");
      }
    }
  }

  void showFailToast() {
    AlarmUtil.showAlertToast("Agent初始化失败,请正确配置");
  }

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
            eventBus.fire(AgentMessageEvent(message: EventBusMessage.delete, agent: AgentBean()..id = id));
            Get.back();
          },
        ));
  }

  void showEmptyLibraryDialog() {
    Get.dialog(barrierDismissible: false, EmptyLibraryDialog());
  }

  void updateHeight(double deltaY) {
    inputPromptHeight.value = (inputPromptHeight.value + deltaY).clamp(minInputPromptHHeight, maxInputPromptHHeight);
  }
}
