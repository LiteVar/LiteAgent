import 'dart:async';
import 'dart:io';

import 'package:audioplayers/audioplayers.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/conversation.dart';
import 'package:lite_agent_client/models/local/message.dart';
import 'package:lite_agent_client/server/local_server/agent_server.dart';
import 'package:lite_agent_client/server/local_server/audio_service.dart';
import 'package:lite_agent_client/server/local_server/parser.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/agent/agent_validator.dart';
import 'package:lite_agent_client/widgets/input_box_container.dart';
import 'package:lite_agent_client/widgets/listview_chat_message.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:path_provider/path_provider.dart';

import '../logic.dart';
import '../../../repositories/conversation_repository.dart';

/// 聊天服务
/// 负责处理所有聊天相关的逻辑
class ChatService {
  final AdjustmentLogic logic;
  
  // 聊天相关属性
  final Rx<ConversationModel> conversation = Rx<ConversationModel>(ConversationModel());
  AgentLocalServer? currentAgentServer;
  late final MessageHandler _messageHandler;
  
  // 音频相关
  final AudioPlayer _audioPlayer = AudioPlayer();
  StreamSubscription? completeSub;
  StreamSubscription? stateSub;
  
  // 控制器
  final inputBoxController = InputBoxController();
  final ChatMessageListViewController listViewController = ChatMessageListViewController();
  
  // 序列化startChat调用以防止并发初始化
  Completer<bool>? _startChatCompleter;
  
  ChatService(this.logic) {
    _initMessageHandler();
  }

  /// 初始化消息处理器
  void _initMessageHandler() {
    inputBoxController
      ..onSendButtonPress = sendMessage
      ..onAudioRecordFinish = onAudioRecordFinish;

    listViewController
      ..onMessageThoughButtonClick = logic.showMessageThoughtDetail
      ..onMessageAudioButtonClick = playMessageAudio;

    _messageHandler = MessageHandler(
      chatMessageList: conversation.value.chatMessageList,
      onThoughtUpdate: (message) {
        if (logic.showThoughtProcessDetail.value && message.taskId == logic.currentThoughtProcessId) {
          logic.showMessageThoughtDetail(message);
        }
      },
      onAgentReply: (message) {
        if (logic.modelStateManager.currentTTSModelId.value.isNotEmpty) {
          int index = conversation.value.chatMessageList.indexOf(message);
          playMessageAudio(index, message.message);
        }
      },
      onHandlerFinished: () {
        conversation.refresh();
        listViewController.scrollListToBottom();
        debugConversationRepository.updateDebugHistory(conversation.value.agentId, conversation.value);
      },
    );
  }

  /// 初始化聊天数据
  Future<void> initChatData(String agentId) async {
    var history = await debugConversationRepository.getDebugHistory(agentId);
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
  }

  /// 确保聊天已启动
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

  /// 发送消息
  Future<void> sendMessage(String message) async {
    if (logic.agent.value?.agentType == AgentValidator.DTO_TYPE_REFLECTION) {
      AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
      return;
    }

    if (message.trim().isEmpty) {
      return;
    }

    // 如果startChat仍在进行中，阻止重复提交
    if (_startChatCompleter != null && !(currentAgentServer?.isConnecting() ?? false)) {
      AlarmUtil.showAlertToast("Agent未初始化完成，请勿重复提交");
      return;
    }

    // 确保startChat完成后再发送
    if (!(await _ensureChatStarted())) {
      logic.showFailToast();
      return;
    }

    var sendMessage = ChatMessageModel()
      ..message = message
      ..roleName = "userName"
      ..sendRole = ChatRoleType.User;
    conversation.value.chatMessageList.add(sendMessage);

    listViewController.scrollListToBottom();
    debugConversationRepository.updateDebugHistory(conversation.value.agentId, conversation.value);

    currentAgentServer?.sendUserMessage(message);
  }

  /// 清除所有消息
  void clearAllMessage() async {
    logic.showThoughtProcessDetail.value = false;
    conversation.value.chatMessageList.clear();
    conversation.refresh();
    await debugConversationRepository.updateDebugHistory(conversation.value.agentId, conversation.value);
    await currentAgentServer?.clearChat();
    currentAgentServer = null;
  }

  /// 播放消息音频
  void playMessageAudio(int index, String content) async {
    if (content.isEmpty) {
      AlarmUtil.showAlertToast("结果为空不可转换");
      return;
    }
    final selectedModel = logic.modelStateManager.currentTTSModel;
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

  /// 音频录制完成处理
  Future<void> onAudioRecordFinish(File recordFile) async {
    final selectedModel = logic.modelStateManager.currentASRModel;
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

  /// 启动聊天
  Future<bool> startChat() async {
    var agent = logic.agent.value;

    await currentAgentServer?.clearChat();

    if (agent?.agentType == AgentValidator.DTO_TYPE_REFLECTION) {
      return false;
    }

    var agentServer = AgentLocalServer();

    if (agent != null) {
      var capabilityDto = await agentServer.buildLocalAgentCapabilityDto(agent, autoModelList: logic.modelStateManager.autoAgentModelList);
      _messageHandler.subAgentNameMap = agentServer.subAgentNameMap;

      if (capabilityDto != null) {
        await agentServer.initChat(capabilityDto);
      }
    }

    if (agentServer.isConnecting()) {
      agentServer.setMessageHandler(_messageHandler);
      currentAgentServer = agentServer;
      return true;
    }
    return false;
  }

  /// 清理资源
  void dispose() {
    _audioPlayer.stop();
    completeSub = null;
    stateSub = null;
    _audioPlayer.dispose();
    inputBoxController.dispose();
    listViewController.dispose();

    currentAgentServer?.clearChat();
    currentAgentServer = null;
    _messageHandler.dispose();
  }
}
