import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
import 'package:opentool_dart/opentool_dart.dart';
import 'package:window_manager/window_manager.dart';

import '../../config/routes.dart';
import '../../models/dto/agent_detail.dart';
import '../../models/local_data_model.dart';
import '../../server/local_server/parser.dart';

class ChatLogic extends GetxController with WindowListener {
  final chatScrollController = ScrollController();
  final TextEditingController chatController = TextEditingController();
  final FocusNode chatFocusNode = FocusNode();

  late StreamSubscription _subscription;
  late StreamSubscription _agentSubscription;
  late StreamSubscription _modelSubscription;

  var conversationList = <AgentConversationBean>[].obs;
  Rx<AgentConversationBean?> currentConversation = Rx<AgentConversationBean?>(null);
  var isShowDrawer = false.obs;
  var selectImagePath = "".obs;

  var messageHoverItemId = "".obs;
  var enableInput = true.obs;

  AgentLocalServer? currentServer;
  AccountDTO? account;

  List<ChatMessage> receivingMessageList = [];

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
    chatFocusNode.dispose();
    chatController.dispose();
    receivingMessageList.clear();
    currentServer?.clearChat();
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
    //conversation.id = DateTime.now().microsecondsSinceEpoch.toString();
    conversation.agentId = agent.id;
    conversation.agent = agent;
    conversation.isCloud = agent.isCloud ?? false;
    conversationList.add(conversation);
    await conversationRepository.updateConversation(conversation.agentId, conversation);
    switchChatView(conversation.agentId, true);
  }

  void sendMessage(String message, String imgPath) async {
    String serverAgentId = currentServer?.agentId ?? "";
    String conversationAgentId = currentConversation.value?.agentId ?? "";
    if (serverAgentId.isEmpty || serverAgentId != conversationAgentId) {
      showFailToast();
      return;
    }
    if (message.trim().isEmpty) {
      return;
    }
    var conversation = currentConversation.value;
    if (conversation != null) {
      var chatMessage = ChatMessage();
      chatMessage.message = message;
      chatMessage.userName = "userName";
      chatMessage.sendRole = ChatRole.User;
      //chatMessage.imgFilePath = imgPath;
      conversation.chatMessageList.add(chatMessage);
      conversation.updateTime = DateTime.now().microsecondsSinceEpoch;
      conversationList.sort((a, b) => (b.updateTime ?? 0) - (a.updateTime ?? 0));

      scrollListToBottom(true);
      await conversationRepository.updateConversation(conversation.agentId, conversation);
      currentConversation.refresh();
    }
  }

  void clearAllMessage() async {
    var conversation = currentConversation.value;
    if (conversation != null) {
      receivingMessageList.clear();
      currentServer?.clearChat();
      conversation.chatMessageList.clear();
      await conversationRepository.updateConversation(conversation.agentId, conversation);
      currentConversation.refresh();
      isShowDrawer.value = false;

      await startChat(true);
    }
  }

  Future<void> switchChatView(String agentId, bool showToast) async {
    receivingMessageList.clear();
    currentServer?.clearChat();
    currentServer = null;
    for (var conversation in conversationList) {
      if (conversation.agentId == agentId) {
        currentConversation.value = conversation;
        isShowDrawer.value = false;
        scrollListToBottom(false);
        break;
      }
    }
    if (currentServer == null || currentServer?.agentId != agentId) {
      await startChat(showToast);
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

  void onChatButtonPress() {
    chatFocusNode.requestFocus();
    var message = chatController.text;
    if (message.trim().isNotEmpty) {
      sendMessage(message, selectImagePath.value);
      if (currentServer?.agentId == currentConversation.value?.agentId) {
        currentServer?.sendUserMessage(message);
      }
      chatController.text = '';
      selectImagePath.value = "";
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

  void onStartChatButtonClick() {
    Get.dialog(SelectAgentDialog(onStartChatConfirm: (AgentDTO agent) {
      var targetAgent = AgentBean();
      targetAgent.translateFromDTO(agent);
      createNewChat(targetAgent);
    }));
  }

  Future<void> startChat(bool showToast) async {
    var conversation = currentConversation.value;
    var agent = conversation?.agent;
    if (agent == null) {
      if (showToast) {
        showFailToast();
      }
      return;
    }
    AgentLocalServer agentServer = AgentLocalServer();
    AgentDetailDTO? agentDTO;
    bool isCloudAgent = agent.isCloud ?? false;
    CapabilityDto? capabilityDto;
    if (isCloudAgent) {
      agentDTO = await agentRepository.getCloudAgentDetail(agent.id);
      enableInput.value = agentDTO?.agent?.type != AgentType.REFLECTION;
      if (!enableInput.value) {
        return;
      }
      if (agentDTO != null) {
        capabilityDto = await agentServer.buildCloudAgentCapabilityDto(agentDTO);
      }
    } else {
      enableInput.value = agent.agentType != AgentType.REFLECTION;
      if (!enableInput.value) {
        return;
      }
      capabilityDto = await agentServer.buildLocalAgentCapabilityDto(agent);
    }

    if (capabilityDto != null) {
      agentServer.agentId = conversation?.agentId ?? "";
      receivingMessageList.clear();
      await agentServer.initChat(capabilityDto, (agentMessage) {
        parseAgentMessage(agentMessage);
        if (currentConversation.value != null) {
          try {
            if (agentMessage.role == ToolRoleType.USER && agentMessage.to == ToolRoleType.AGENT) {
              //create Agent message model
              var receivedMessage = ChatMessage();
              receivedMessage.sendRole = ChatRole.Agent;
              receivedMessage.taskId = agentMessage.taskId;
              receivedMessage.isLoading = true;
              currentConversation.value?.chatMessageList.add(receivedMessage);
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
            conversationRepository.updateConversation(currentConversation.value!.agentId, currentConversation.value!);
            currentConversation.refresh();
            scrollListToBottom(false);
          } catch (e) {
            print(e.toString());
          }
        }
      });
    }

    if (agentServer.isConnecting()) {
      currentServer = agentServer;
    } else {
      if (showToast) {
        showFailToast();
      }
    }
  }

  ChatMessage? getTargetMessage(String taskId) {
    for (var message in receivingMessageList) {
      if (message.taskId == taskId) {
        return message;
      }
    }
    return null;
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

  void copyToClipboard(String string) {
    Clipboard.setData(ClipboardData(text: string)).then((text) => AlarmUtil.showAlertToast("复制成功"));
  }
}
