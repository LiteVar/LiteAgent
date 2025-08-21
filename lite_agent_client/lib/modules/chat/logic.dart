import 'dart:async';
import 'dart:io';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/account.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/conversation_repository.dart';
import 'package:lite_agent_client/server/local_server/agent_server.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/utils/web_util.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_agent.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:path_provider/path_provider.dart';
import 'package:window_manager/window_manager.dart';

import '../../config/routes.dart';
import '../../models/dto/agent_detail.dart';
import '../../models/local_data_model.dart';
import '../../repositories/model_repository.dart';
import '../../repositories/tool_repository.dart';
import '../../server/local_server/audio_service.dart';
import '../../server/local_server/parser.dart';
import '../../widgets/input_box_container.dart';
import '../../widgets/listview_chat_message.dart';

class ChatLogic extends GetxController with WindowListener {
  late StreamSubscription _subscription;
  late StreamSubscription _agentSubscription;
  late StreamSubscription _modelSubscription;

  var conversationList = <AgentConversationBean>[].obs;
  Rx<AgentConversationBean?> currentConversation = Rx<AgentConversationBean?>(null);
  var isShowDrawer = false.obs;
  var selectImagePath = "".obs;

  var messageHoverItemId = "".obs;

  MessageHandler? _messageHandler;
  AgentLocalServer? currentServer;
  AccountDTO? account;

  //var currentThoughtList = <Thought>[].obs;
  var currentSubMessageList = <ChatMessage>[].obs;
  var currentThoughtProcessId = "";
  var showThoughtProcessDetail = false.obs;

  final AudioPlayer _audioPlayer = AudioPlayer();
  StreamSubscription? completeSub;
  StreamSubscription? stateSub;
  ModelBean? currentTTSModel;
  ModelBean? currentASRModel;

  final inputBoxController = InputBoxController();
  final ChatMessageListViewController listViewController = ChatMessageListViewController();

  var conversationItemHoverId = "".obs;

  // Serialize startChat calls
  Completer<bool>? _startChatCompleter;

  // Ensure startChat finishes before sending message. Coalesces concurrent calls.
  Future<bool> _ensureChatStarted() async {
    if (currentServer?.isConnecting() ?? false) return true;
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
  void onInit() {
    super.onInit();
    initWindow();
    initData();
    initController();
    initEventBus();
  }

  void initWindow() async {
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  void initData() async {
    /*if (await accountRepository.isLogin()) {
      account.value = await accountRepository.getAccountInfoFromBox();
    }*/
    var list = await agentRepository.getCloudAgentList(0);
    conversationList.assignAll(await conversationRepository.getConversationListFromBox());
    for (var info in conversationList) {
      if (info.isCloud) {
        for (var agent in list) {
          if (agent.id == info.agentId) {
            var bean = AgentBean();
            info.agent = bean;
            bean.translateFromDTO(agent);
            break;
          }
        }
      } else {
        info.agent = await agentRepository.getAgentFromBox(info.agentId);
      }
    }
    conversationList.removeWhere((info) => info.agent == null);
    //var list = await agentRepository.getCloudAgentConversationList();
    conversationList.refresh();
  }

  void initController() {
    inputBoxController
      ..onSendButtonPress = sendMessage
      ..onAudioRecordFinish = onAudioRecordFinish;

    listViewController
      ..onMessageAudioButtonClick = playMessageAudio
      ..onMessageThoughButtonClick = showMessageThoughtDetail;
  }

  void initEventBus() {
    _subscription = eventBus.on<MessageEvent>().listen((event) {
      if (event.message == EventBusMessage.login) {
        initData();
        //conversationList.refresh();
      } else if (event.message == EventBusMessage.logout) {
        account = null;
        conversationList.removeWhere((info) => info.isCloud);
        conversationList.refresh();
        if (currentConversation.value?.isCloud ?? false) {
          currentConversation.value = null;
        }
        currentConversation.refresh();
      } else if (event.message == EventBusMessage.updateSingleData && event.data is AccountDTO) {
        account = event.data;
        currentConversation.refresh();
      } else if (event.message == EventBusMessage.sync) {
        updateCloudAgentName();
        if (currentConversation.value?.isCloud ?? false) {
          var agentId = currentConversation.value?.agent?.id ?? '';
          if (agentId.isNotEmpty) {
            switchChatView(agentId, false);
          }
          currentConversation.refresh();
        }
      }
    });
    _agentSubscription = eventBus.on<AgentMessageEvent>().listen((event) async {
      if (event.message == EventBusMessage.updateSingleData && event.agent != null) {
        var agent = event.agent;
        if (agent != null) {
          for (var info in conversationList) {
            if (info.agentId == agent.id) {
              info.agent = agent;
              break;
            }
          }
          conversationList.refresh();
          if ((currentConversation.value?.agent?.id ?? "") == agent.id) {
            if (currentServer?.agentId == agent.id) {
              switchChatView(agent.id, false);
            }
            currentConversation.refresh();
          }
        }
      } else if (event.message == EventBusMessage.startChat && event.agent != null) {
        var agent = event.agent;
        if (agent != null) {
          createNewChat(agent);
        }
      } else if (event.message == EventBusMessage.delete && event.agent != null) {
        var agent = event.agent;
        if (agent != null) {
          conversationList.removeWhere((info) => info.agentId == agent.id);
          conversationList.refresh();
          conversationRepository.removeConversation(agent.id);
          conversationRepository.removeAdjustmentConversation(agent.id);
        }
      }
    });
    _modelSubscription = eventBus.on<ModelMessageEvent>().listen((event) async {
      if (event.message == EventBusMessage.updateSingleData && event.model != null) {
        var model = event.model;
        var agent = currentConversation.value?.agent;
        if (model != null && agent != null) {
          if (agent.modelId == model.id) {
            switchChatView(agent.id, false);
          }
        }
      }
    });
  }

  @override
  void onClose() {
    _subscription.cancel();
    _agentSubscription.cancel();
    _modelSubscription.cancel();
    currentServer?.clearChat();
    currentServer = null;
    currentTTSModel = null;
    currentASRModel = null;
    _audioPlayer.stop();
    completeSub = null;
    stateSub = null;
    _audioPlayer.dispose();
    inputBoxController.dispose();
    listViewController.dispose();
    _messageHandler?.dispose();

    windowManager.removeListener(this);
    super.onClose();
  }

  @override
  void onWindowClose() {
    Get.back();
  }

  void createNewChat(AgentBean agent) async {
    var isAgentCloud = agent.isCloud ?? false;
    if (isAgentCloud) {
      var cloudAgent = await agentRepository.getCloudAgentDetail(agent.id);
      if (cloudAgent?.model == null) {
        AlarmUtil.showAlertDialog("ai模型初始化失败,请正确配置模型");
        return;
      }
      if (cloudAgent?.agent?.type == AgentType.REFLECTION) {
        AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
        return;
      }
    } else {
      if (agent.modelId.isEmpty) {
        AlarmUtil.showAlertDialog("ai模型初始化失败,请正确配置模型");
        return;
      }
      if (agent.agentType == AgentType.REFLECTION) {
        AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
        return;
      }
    }
    Get.back();
    for (var conversation in conversationList) {
      if (agent.id == conversation.agentId) {
        switchChatView(conversation.agentId, true);
        return;
      }
    }
    AgentConversationBean conversation = AgentConversationBean();
    conversation.agentId = agent.id;
    conversation.agent = agent;
    conversation.isCloud = agent.isCloud ?? false;
    conversationList.add(conversation);
    await conversationRepository.updateConversation(conversation.agentId, conversation);
    switchChatView(conversation.agentId, true);
  }

  Future<void> sendMessage(String message) async {
    AgentBean? agent = currentConversation.value?.agent;
    if (agent != null && agent.agentType == AgentType.REFLECTION) {
      AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
      return;
    }

    if (message.trim().isEmpty) {
      return;
    }

    // If startChat is still in progress, block repeated submissions
    if (_startChatCompleter != null && !(currentServer?.isConnecting() ?? false)) {
      AlarmUtil.showAlertToast("Agent未初始化完成，请勿重复提交");
      return;
    }

    // Ensure startChat completes before sending (handles rapid consecutive sends)
    if (!(await _ensureChatStarted())) {
      showFailToast();
      return;
    }

    var conversation = currentConversation.value;
    if (conversation != null) {
      var sendMessage = ChatMessage()
        ..message = message
        ..roleName = "userName"
        ..sendRole = ChatRoleType.User;
      conversation.chatMessageList.add(sendMessage);

      conversation.updateTime = DateTime.now().microsecondsSinceEpoch;
      conversationList.sort((a, b) => (b.updateTime ?? 0) - (a.updateTime ?? 0));

      listViewController.scrollListToBottom();
      await conversationRepository.updateConversation(conversation.agentId, conversation);
      currentConversation.refresh();

      if (currentServer?.agentId == conversation.agentId) {
        currentServer?.sendUserMessage(message);
      }
    }
  }

  Future<void> onAudioRecordFinish(File recordFile) async {
    var asrModel = currentASRModel;
    if (asrModel == null) {
      AlarmUtil.showAlertToast("请正确配置ASR模型");
      return;
    }

    try {
      inputBoxController.setAudioEnable(false);
      EasyLoading.show(status: "正在转换...");
      String message = await AudioService.speechToText(
        llmConfig: LLMConfig(model: asrModel.name, baseUrl: asrModel.url, apiKey: asrModel.key),
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
      listViewController.activeAudioMessageId = "";
      inputBoxController.setAudioEnable(true);
      EasyLoading.dismiss();
      AlarmUtil.showAlertToast("转换出错");
      throw Exception('语音转文本失败: $e');
    }
  }

  void playMessageAudio(int index, String content) async {
    if (content.isEmpty) {
      AlarmUtil.showAlertToast("结果为空不可转换");
      return;
    }
    var conversation = currentConversation.value;

    var ttsModel = currentTTSModel;
    if (ttsModel == null) {
      AlarmUtil.showAlertToast("请正确配置TTS模型");
      return;
    }

    try {
      if (_audioPlayer.state == PlayerState.playing) {
        _audioPlayer.stop();
        if (listViewController.activeAudioMessageId == index.toString()) {
          return;
        }
      }

      String agentId = conversation?.agentId ?? "";

      EasyLoading.show(status: "正在转换...");
      var file = await AudioService.textToSpeech(
          llmConfig: LLMConfig(model: ttsModel.name, baseUrl: ttsModel.url, apiKey: ttsModel.key),
          text: content,
          outputDirectory: await getTemporaryDirectory());

      //make sure the conversation is the same
      if (agentId != currentConversation.value?.agentId) {
        EasyLoading.dismiss();
        return;
      }

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

  void clearAllMessage() async {
    var conversation = currentConversation.value;
    if (conversation != null) {
      currentServer?.clearChat();
      currentServer = null;
      conversation.chatMessageList.clear();
      await conversationRepository.updateConversation(conversation.agentId, conversation);
      currentConversation.refresh();
      isShowDrawer.value = false;

      showThoughtProcessDetail.value = false;
    }
  }

  Future<void> switchChatView(String agentId, bool showToast) async {
    _audioPlayer.stop();
    listViewController.activeAudioMessageId = '';
    currentServer?.clearChat();
    currentServer = null;
    currentTTSModel = null;
    currentASRModel = null;
    for (var conversation in conversationList) {
      if (conversation.agentId == agentId) {
        currentConversation.value = conversation;
        isShowDrawer.value = false;
        Future.delayed(const Duration(milliseconds: 100), () {
          listViewController.scrollListToBottom(animate: false);
          // 再次延迟确保滚动完成
          Future.delayed(const Duration(milliseconds: 100), () => listViewController.scrollListToBottom(animate: false));
        });
        break;
      }
    }
    conversationList.refresh();
    showThoughtProcessDetail.value = false;

    updateListViewAndInputBoxUI();
  }

  Future<void> updateListViewAndInputBoxUI() async {
    var conversation = currentConversation.value;
    var agent = conversation?.agent;
    if (agent != null) {
      if (agent.isCloud == true) {
        var agentDTO = await agentRepository.getCloudAgentDetail(agent.id);

        if (agentDTO != null) {
          var hasTTSModel = agentDTO.ttsModel != null;
          listViewController.setAudioButtonVisible(hasTTSModel);

          var enableInput = agentDTO.agent?.type != AgentType.REFLECTION;
          inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");

          var hasASRModel = agentDTO.asrModel != null;
          inputBoxController.setEnableAudioInput(hasASRModel);

          if (hasTTSModel) {
            String baseUrl = agentDTO.ttsModel!.baseUrl;
            if (baseUrl.endsWith("/v1")) baseUrl = baseUrl.substring(0, baseUrl.length - 3);

            currentTTSModel = ModelBean()
              ..name = agentDTO.ttsModel!.name
              ..url = baseUrl
              ..key = agentDTO.ttsModel!.apiKey;
          }

          if (hasASRModel) {
            String baseUrl = agentDTO.asrModel!.baseUrl;
            if (baseUrl.endsWith("/v1")) baseUrl = baseUrl.substring(0, baseUrl.length - 3);

            currentASRModel = ModelBean()
              ..name = agentDTO.asrModel!.name
              ..url = baseUrl
              ..key = agentDTO.asrModel!.apiKey;
          }
        }
      } else {
        var hasTTSModel = agent.ttsModelId != null && agent.ttsModelId!.isNotEmpty;
        listViewController.setAudioButtonVisible(hasTTSModel);

        var enableInput = agent.agentType != AgentType.REFLECTION;
        inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");

        var hasASRModel = agent.asrModelId != null && agent.asrModelId!.isNotEmpty;
        inputBoxController.setEnableAudioInput(hasASRModel);

        if (hasTTSModel) currentTTSModel = await modelRepository.getModelFromBox(agent.ttsModelId!);
        if (hasASRModel) currentASRModel = await modelRepository.getModelFromBox(agent.asrModelId!);
      }
    }
  }

  void showAgentInfo() {
    isShowDrawer.value = !isShowDrawer.value;
  }

  void closeAgentInfo() {
    isShowDrawer.value = false;
  }

  void jumpToAdjustPage() {
    AgentBean? agent = currentConversation.value?.agent;
    if (agent != null) {
      if (agent.isCloud ?? false) {
        WebUtil.openAgentAdjustUrl(agent.id);
      } else {
        Get.toNamed(Routes.adjustment, arguments: agent);
      }
      isShowDrawer.value = false;
    }
  }

  void onStartChatButtonClick() {
    Get.dialog(SelectAgentDialog(onStartChatConfirm: (AgentDTO agent) {
      var targetAgent = AgentBean();
      targetAgent.translateFromDTO(agent);
      createNewChat(targetAgent);
    }));
  }

  Future<bool> startChat() async {
    var conversation = currentConversation.value;
    var agent = conversation?.agent;
    if (agent == null) {
      return false;
    }
    AgentLocalServer agentServer = AgentLocalServer();
    AgentDetailDTO? agentDTO;
    bool isCloudAgent = agent.isCloud ?? false;
    CapabilityDto? capabilityDto;
    if (isCloudAgent) {
      agentDTO = await agentRepository.getCloudAgentDetail(agent.id);

      if (agentDTO != null) {
        var enableInput = agentDTO.agent?.type != AgentType.REFLECTION;
        inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");
        if (!enableInput) {
          return false;
        }

        capabilityDto = await agentServer.buildCloudAgentCapabilityDto(agentDTO);
      }
    } else {
      var isAutoAgent = agent.autoAgentFlag ?? false;
      var autoAgentModelList = <ModelBean>[];
      if (isAutoAgent) {
        var allModels = await modelRepository.getModelListFromBox();
        var modelList = <ModelBean>[];
        modelList.assignAll(allModels.where((model) => model.type == "LLM" || model.type == null));
        autoAgentModelList.assignAll(modelList.where((model) => model.supportMultiAgent == true));

        var autoFunctionList = <AgentToolFunction>[];
        var toolList = await toolRepository.getToolListFromBox();
        var autoAgentToolList = toolList.where((tool) => tool.supportMultiAgent == true).toList();
        for (var tool in autoAgentToolList) {
          await tool.initToolFunctionList();
          for (var function in tool.functionList) {
            function.toolName = tool.name;
            autoFunctionList.add(function);
          }
        }
        agent.functionList ??= [];
        agent.functionList?.assignAll(autoFunctionList);
      }

      var enableInput = agent.agentType != AgentType.REFLECTION;
      inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");
      if (!enableInput) {
        return false;
      }

      capabilityDto = await agentServer.buildLocalAgentCapabilityDto(agent, autoModelList: isAutoAgent ? autoAgentModelList : null);
    }

    if (capabilityDto != null) {
      agentServer.agentId = conversation?.agentId ?? "";
      _messageHandler?.dispose();

      await agentServer.initChat(capabilityDto);
    }

    if (agentServer.isConnecting()) {
      MessageHandler messageHandler = MessageHandler(
        chatMessageList: currentConversation.value?.chatMessageList ?? [],
        onThoughtUpdate: (message) {
          if (showThoughtProcessDetail.value && message.taskId == currentThoughtProcessId) {
            showMessageThoughtDetail(message);
          }
        },
        onAgentReply: (message) {
          var ttsModelId = agent.ttsModelId ?? "";
          if (ttsModelId.isNotEmpty) {
            int index = currentConversation.value?.chatMessageList.indexOf(message) ?? -1;
            playMessageAudio(index, message.message);
          }
        },
        subAgentNameMap: agentServer.subAgentNameMap,
        onHandlerFinished: () {
          var conversation = currentConversation.value;
          if (conversation != null) {
            conversationRepository.updateConversation(conversation.agentId, conversation);
          }
          currentConversation.refresh();
          listViewController.scrollListToBottom();
        },
      );
      agentServer.setMessageHandler(messageHandler);
      _messageHandler = messageHandler;
      currentServer = agentServer;
      return true;
    }
    return false;
  }

  Future<void> updateCloudAgentName() async {
    var list = await agentRepository.getCloudAgentList(0);
    for (var info in conversationList) {
      if (info.isCloud) {
        for (var agent in list) {
          if (agent.id == info.agentId) {
            var bean = AgentBean();
            info.agent = bean;
            bean.translateFromDTO(agent);
            break;
          }
        }
      }
    }
    conversationList.refresh();
  }

  void showFailToast() {
    AlarmUtil.showAlertToast("Agent初始化失败,请正确配置");
  }

  Future<void> deleteConversation(String conversationId) async {
    conversationList.removeWhere((info) => info.agentId == conversationId);
    await conversationRepository.removeConversation(conversationId);

    if (currentConversation.value?.agentId == conversationId) {
      showThoughtProcessDetail.value = false;
      currentConversation.value = null;
      currentServer?.clearChat();
      currentServer = null;
      _messageHandler?.dispose();
      currentTTSModel = null;
      currentASRModel = null;
    }
    conversationList.refresh();
  }
}
