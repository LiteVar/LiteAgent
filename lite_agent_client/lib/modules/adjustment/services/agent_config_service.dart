import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/agent/agent_validator.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/utils/extension/tool_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';
import 'package:lite_agent_client/utils/snowflake_util.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';
import '../../../config/constants.dart';

import '../logic.dart';

/// Agent配置服务
/// 负责处理Agent配置相关的数据操作和业务逻辑
class AgentConfigService {
  final AdjustmentLogic logic;

  AgentConfigService(this.logic);

  /// 初始化数据
  Future<void> initData(String agentId) async {
    await logic.chatService.initChatData(agentId);

    logic.isLogin = await accountRepository.isLogin();
    if (logic.isLogin) {
      var account = await accountRepository.getAccountInfoFromBox();
      logic.chatService.listViewController.agentAvatarPath = account?.avatar ?? "";
    }
    var agent = await agentRepository.getAgentFromBox(agentId);
    logic.agent.value = agent;
    if (agent != null) {
      await _loadAgentData(agent);
    }
  }

  /// 加载Agent数据
  Future<void> _loadAgentData(AgentModel agent) async {
    var isAutoAgent = agent.autoAgentFlag ?? false;
    logic.chatService.listViewController.agentAvatarPath = agent.iconPath;

    var allModels = await modelRepository.getModelListFromBox();

    // 同步到agent状态管理器
    logic.agentStateManager.updateTemperature(agent.temperature.toDouble());
    logic.agentStateManager.updateMaxToken(agent.maxToken.toDouble());
    logic.agentStateManager.updateTopP(agent.topP);

    var modelList = allModels.where((model) => model.type == ModelValidator.LLM || model.type == null).toList();
    if (isAutoAgent) {
      var autoAgentModelList = modelList.where((model) => model.supportMultiAgent == true).toList();
      logic.modelStateManager.updateAutoAgentModelList(autoAgentModelList);
    }
    // 同步到模型数据管理器
    logic.modelStateManager.updateLLMModelList(modelList);
    logic.modelStateManager.updateTTSModelList(allModels.where((model) => model.type?.toLowerCase() == ModelValidator.TTS).toList());
    logic.modelStateManager.updateASRModelList(allModels.where((model) => model.type?.toLowerCase() == ModelValidator.ASR).toList());


    if (agent.modelId.isNotEmpty) {
      selectModel(agent.modelId, true);
    }
    logic.promptController.text = agent.prompt;

    await _loadToolData(agent, isAutoAgent);
    await _loadLibraryData(agent, isAutoAgent);
    await _loadVoiceData(agent);
    await _loadChildAgentData(agent);

    // 同步到agent状态管理器
    logic.agentStateManager.updateAgentType(agent.agentType ?? AgentValidator.DTO_TYPE_GENERAL);
    logic.agentStateManager.updateOperationMode(agent.operationMode ?? OperationMode.PARALLEL);

    var enableInput = agent.agentType != AgentValidator.DTO_TYPE_REFLECTION;
    logic.chatService.inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");
    if (!enableInput) {
      logic.chatService.inputBoxController.switchInputWay(false);
    }

    if (isAutoAgent) {
      updateAgentInfo(showToast: false);
      logic.modelStateManager.toggleAudioExpanded(true);
      logic.toolStateManager.toggleToolExpanded(true);
      logic.modelStateManager.toggleModelExpanded(true);
    }
  }

  /// 加载工具数据
  Future<void> _loadToolData(AgentModel agent, bool isAutoAgent) async {
    var toolList = await toolRepository.getToolListFromBox();
    if (isAutoAgent) {
      var autoAgentToolList = toolList.where((tool) => tool.supportMultiAgent == true).toList();
      for (var tool in autoAgentToolList) {
        try {
          await tool.initFunctions();
          for (var function in tool.functionList) {
            function.toolName = tool.name;
            logic.functionList.add(function);
          }
        } catch (e) {
          Log.e("initToolFunctionList error: $e");
        }
      }
    } else {
      Map<String, ToolModel?> toolMap = {};
      for (var tool in toolList) {
        //avoid toolFunction select dialog has cache
        tool.functionList.clear();
        toolMap[tool.id] = tool;
      }
      if (agent.functionList != null) {
        for (var function in agent.functionList!) {
          ToolModel? tool = toolMap[function.toolId];
          if (tool != null) {
            //update toolName
            function.toolName = tool.name;
            logic.functionList.add(function);
          }
        }
      }
    }

    // 同步工具数据到状态管理器
    logic.toolStateManager.functionList.assignAll(logic.functionList);
    logic.toolStateManager.toolOperationMode.value = agent.toolOperationMode ?? OperationMode.PARALLEL;
  }

  /// 加载知识库数据
  Future<void> _loadLibraryData(AgentModel agent, bool isAutoAgent) async {
    if (logic.isLogin && !isAutoAgent) {
      var validLibraryIdsForExport = <String>[];
      if (agent.libraryIds != null) {
        for (var id in agent.libraryIds!) {
          var library = await libraryRepository.getLibraryById(id);
          if (library != null) {
            logic.libraryStateManager.addLibrary(library);
            validLibraryIdsForExport.add(library.id);
          }
        }
      }
      agent.libraryIds = validLibraryIdsForExport;
      logic.libraryStateManager.libraryList.refresh();
    }
  }

  /// 加载语音数据
  Future<void> _loadVoiceData(AgentModel agent) async {
    if (agent.ttsModelId != null && agent.ttsModelId!.isNotEmpty) {
      logic.modelStateManager.selectTTSModel(agent.ttsModelId!);
      logic.modelStateManager.toggleTextToSpeech(true);
      logic.chatService.listViewController.setAudioButtonVisible(true);
    }
    if (agent.asrModelId != null && agent.asrModelId!.isNotEmpty) {
      logic.modelStateManager.selectASRModel(agent.asrModelId!);
      logic.modelStateManager.toggleSpeechToText(true);
      logic.chatService.inputBoxController.setEnableAudioInput(true);
    }
  }

  /// 加载子Agent数据
  Future<void> _loadChildAgentData(AgentModel agent) async {
    if (agent.childAgentIds != null) {
      for (var id in agent.childAgentIds!) {
        var childAgent = await agentRepository.getAgentFromBox(id);
        if (childAgent != null) {
          logic.agentStateManager.addChildAgent(childAgent);
        }
      }
    }
  }

  /// 选择模型
  void selectModel(String modelId, bool isInit) {
    for (var model in logic.modelStateManager.llmModelList) {
      if (model.id == modelId) {
        logic.modelStateManager.selectModel(model);
        logic.modelStateManager.selectLLMModel(modelId);
        String maxTokenString = model.maxToken ?? "4096";
        int maxToken = int.parse(maxTokenString);
        if (logic.agentStateManager.sliderTokenValue.value > maxToken) {
          logic.agentStateManager.updateMaxToken(maxToken.toDouble());
        }
        if (!isInit) {
          logic.isAgentChangeWithoutSave = true;
        }
        logic.modelStateManager.llmModelList.refresh();
        break;
      }
    }
  }

  /// 创建模型
  Future<String> createModel(ModelFormData modelData) async {
    ModelData model = ModelData(
      id: snowFlakeUtil.getId(),
      createTime: DateTime.now().microsecondsSinceEpoch,
      name: modelData.name,
      url: modelData.baseUrl,
      key: modelData.apiKey,
      type: modelData.modelType,
      alias: modelData.alias,
      maxToken: modelData.maxToken,
      supportDeepThinking: modelData.supportDeepThinking,
      supportMultiAgent: modelData.supportMultiAgent,
      supportToolCalling: modelData.supportToolCalling,
    );
    logic.modelStateManager.llmModelList.add(model);
    await modelRepository.updateModel(model.id, model);
    eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateList));
    return model.id;
  }

  /// 处理创建模型
  Future<String> handleCreateModel(ModelFormData modelData) async {
    String modelId = await createModel(modelData);
    selectModel(modelId, false);
    return modelId;
  }

  /// 更新Agent信息
  void updateAgentInfo({showToast = true}) async {
    logic.showThoughtProcessDetail.value = false;
    try {
      var agent = logic.agent.value;
      if (agent != null && agent.id.isNotEmpty) {
        agent.modelId = logic.modelStateManager.currentModel?.id ?? "";
        agent.prompt = logic.promptController.text;
        agent.temperature = (logic.agentStateManager.sliderTempValue.value * 10).round() / 10;
        agent.maxToken = logic.agentStateManager.sliderTokenValue.value.toInt();
        agent.topP = (logic.agentStateManager.sliderTopPValue.value * 10).round() / 10;
        agent.functionList ??= [];
        agent.functionList?.assignAll(logic.toolStateManager.functionList);
        if (logic.isLogin) {
          agent.libraryIds = logic.libraryStateManager.libraryList.map((library) => library.id).toList();
        }
        agent.agentType = logic.agentStateManager.agentType.value;
        var enableInput = agent.agentType != AgentValidator.DTO_TYPE_REFLECTION;
        logic.chatService.inputBoxController.setEnableInput(enableInput, "反思Agent不能进行聊天对话");
        if (!enableInput) {
          logic.chatService.inputBoxController.switchInputWay(false);
        }
        agent.operationMode = logic.agentStateManager.operationMode.value;
        agent.toolOperationMode = logic.toolStateManager.toolOperationMode.value;
        agent.childAgentIds ??= [];
        agent.childAgentIds?.assignAll(logic.agentStateManager.childAgentList.map((childAgent) => childAgent.id).toList());

        agent.ttsModelId = logic.modelStateManager.currentTTSModelId.value;
        agent.asrModelId = logic.modelStateManager.currentASRModelId.value;

        await agentRepository.updateAgent(agent.id, agent);
        eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateSingleData, agent: agent));
      }
      if (showToast) {
        AlarmUtil.showAlertToast("保存成功");
      }
      logic.isAgentChangeWithoutSave = false;

      logic.chatService.currentAgentServer?.clearChat();
      logic.chatService.currentAgentServer = null;
    } catch (e) {
      Log.e("updateAgentInfo error: $e");
      if (showToast) {
        AlarmUtil.showAlertToast("保存失败");
      }
    }
  }
}
