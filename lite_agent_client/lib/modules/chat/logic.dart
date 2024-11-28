import 'dart:async';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/account.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/conversation_repository.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/server/local_server/agent_server.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/utils/file_util.dart';
import 'package:lite_agent_client/utils/web_util.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_agent.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:window_manager/window_manager.dart';

import '../../config/routes.dart';
import '../../models/dto/agent_detail.dart';
import '../../models/local_data_model.dart';
import '../../widgets/dialog/dialog_tool_edit.dart';

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

  AgentLocalServer? currentServer;
  Rx<AccountDTO?> account = Rx<AccountDTO?>(null);

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
    if (await accountRepository.isLogin()) {
      account.value = await accountRepository.getAccountInfoFromBox();
    }
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
        account.value = null;
        conversationList.removeWhere((info) => info.isCloud);
        conversationList.refresh();
        String agentId = currentConversation.value?.agentId ?? "";
        bool isCloud = !agentId.isNumericOnly;
        if (isCloud) {
          currentConversation.value = null;
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
              currentServer?.clearChat();
              currentServer = null;
              switchChatView(agent.id);
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
            currentServer?.clearChat();
            currentServer = null;
            switchChatView(agent.id);
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
    } else {
      if (agent.modelId.isEmpty) {
        AlarmUtil.showAlertDialog("ai模型初始化失败,请正确配置模型");
        return;
      }
    }
    Get.back();
    for (var conversation in conversationList) {
      if (agent.id == conversation.agentId) {
        switchChatView(conversation.agentId);
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
    switchChatView(conversation.agentId);
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
      chatMessage.imgFilePath = imgPath;
      conversation.chatMessageList.add(chatMessage);
      conversation.updateTime = DateTime.now().microsecondsSinceEpoch;
      print("conversation.updateTime:${conversation.updateTime}");

      scrollListToBottom(true);
      await conversationRepository.updateConversation(conversation.agentId, conversation);
      currentConversation.refresh();
    }
  }

  void clearAllMessage() async {
    var conversation = currentConversation.value;
    if (conversation != null) {
      currentServer?.clearChat();
      conversation.chatMessageList.clear();
      await conversationRepository.updateConversation(conversation.agentId, conversation);
      currentConversation.refresh();
      isShowDrawer.value = false;
    }
  }

  Future<void> switchChatView(String agentId) async {
    for (var conversation in conversationList) {
      if (conversation.agentId == agentId) {
        currentConversation.value = conversation;
        isShowDrawer.value = false;
        scrollListToBottom(false);
        break;
      }
    }
    if (currentServer == null || currentServer?.agentId != agentId) {
      await startChat(true);
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

  void selectImageFile() async {
    String path = await fileUtils.saveImage(100) ?? "";
    selectImagePath.value = path;
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
    ModelBean? model;
    AgentDetailDTO? agentDTO;
    bool isCloudAgent = agent.isCloud ?? false;
    //print("agent:isCloudAgent:$isCloudAgent");
    if (isCloudAgent) {
      agentDTO = await agentRepository.getCloudAgentDetail(agent.id);
      if (agentDTO != null) {
        var dto = agentDTO.model;
        if (dto != null) {
          model = ModelBean();
          model.url = dto.baseUrl;
          model.key = dto.apiKey;
          model.name = dto.name;
        }
      }
    } else {
      String selectedModelId = agent.modelId;
      model = await modelRepository.getModelFromBox(selectedModelId);
    }
    if (model == null) {
      if (showToast) {
        showFailToast();
      }
      return;
    }
    LLMConfigDto llmConfig = LLMConfigDto(
        baseUrl: model.url,
        apiKey: model.key,
        model: model.name,
        temperature: agent.temperature,
        maxTokens: agent.maxToken,
        topP: agent.topP);

    String prompt = agent.prompt;

    List<OpenSpecDto> openSpecList = [];
    if (agentDTO != null) {
      var tools = agentDTO.toolList;
      if (tools != null) {
        for (var tool in tools) {
          ApiKeyDto? apiKey;
          if (tool.apiKeyType == "basic" || tool.apiKeyType == "Basic") {
            apiKey = ApiKeyDto(type: ApiKeyType.basic, apiKey: tool.apiKey ?? "");
          } else if (tool.apiKeyType == "bearer" || tool.apiKeyType == "Bearer") {
            apiKey = ApiKeyDto(type: ApiKeyType.bearer, apiKey: tool.apiKey ?? "");
          }
          String protocol = "";
          switch (tool.schemaType) {
            case 1: //openapi
              protocol = Protocol.openapi;
              break;
            case 2: //jsonrpc
              protocol = Protocol.jsonrpcHttp;
              break;
            case 3: //open_modbus
              protocol = Protocol.openmodbus;
              break;
          }
          OpenSpecDto openSpec = OpenSpecDto(openSpec: tool.schemaStr ?? "", protocol: protocol, apiKey: apiKey);
          openSpecList.add(openSpec);
        }
      }
    } else {
      for (var id in agent.toolList) {
        var tool = await toolRepository.getToolFromBox(id);
        if (tool != null) {
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
          OpenSpecDto openSpec = OpenSpecDto(openSpec: tool.schemaText, protocol: protocol, apiKey: apiKey);
          openSpecList.add(openSpec);
        }
      }
    }

    CapabilityDto capabilityDto = CapabilityDto(llmConfig: llmConfig, systemPrompt: prompt, openSpecList: openSpecList, timeoutSeconds: 20);

    AgentLocalServer agentServer = AgentLocalServer();
    agentServer.agentId = conversation?.agentId ?? "";
    await agentServer.initChat(capabilityDto);
    await agentServer.connectChat((message) {
      if (conversation != null) {
        conversation.chatMessageList.add(message);
        conversationRepository.updateConversation(conversation.agentId, conversation);
        currentConversation.refresh();
        scrollListToBottom(false);
      }
    });
    if (agentServer.isConnecting()) {
      currentServer = agentServer;
      //serverList.add(agentServer);
    } else {
      if (showToast) {
        showFailToast();
      }
    }
  }

  void showFailToast() {
    AlarmUtil.showAlertToast("ai模型初始化失败,请正确配置模型");
  }
}
