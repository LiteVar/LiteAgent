import 'package:get/get.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/model/model_import.dart';
import 'package:lite_agent_client/utils/tool/tool_import.dart';
import 'package:lite_agent_client/utils/batch_id_generator.dart';
import 'package:lite_agent_client/utils/tool/tool_function_parser.dart';
import 'package:lite_agent_client/utils/extension/agent_extension.dart';
import 'package:lite_agent_client/utils/extension/tool_extension.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';
import '../../../models/local/function.dart';
import '../../../utils/agent/agent_validator.dart';
import 'parsing_service.dart';

/// 导入服务
class ImportService extends GetxService {
  late final ParsingService parsingService;

  // 导入状态
  final importError = RxnString();
  final isImporting = false.obs;

  // 导入进度消息
  final importProgressMessages = <String>[].obs;

  @override
  void onInit() {
    super.onInit();
    parsingService = Get.find<ParsingService>();
  }

  /// 重置导入状态
  void reset() {
    importError.value = null;
    isImporting.value = false;
    importProgressMessages.clear();
  }

  /// 执行导入流程
  Future<void> executeImport() async {
    try {
      isImporting.value = true;
      importError.value = null;
      importProgressMessages.clear();

      // 1. 数据验证
      importProgressMessages.add('[任务] 正在验证数据...');
      final validationError = await _validateAllData();
      if (validationError != null) {
        importError.value = validationError;
        importProgressMessages.add('[错误] 数据验证失败: $validationError');
        return;
      }
      importProgressMessages.add('[完成] 数据验证通过。');

      // 2. 导入知识库（最先执行，失败后立即终止导入流程）
      final token = parsingService.knowledgeBaseUploadResult?.token;
      if (token != null && token.isNotEmpty) {
        _updateUploadData();
        importProgressMessages.add('[任务] 正在保存知识库到服务器...');
        final knowledgeBaseResult = await _importKnowledgeBases();
        if (knowledgeBaseResult != null) {
          importError.value = knowledgeBaseResult;
          importProgressMessages.add('[错误] 知识库导入失败: $knowledgeBaseResult');
          return;
        }
        importProgressMessages.add('[完成] 知识库导入完成。');
      }

      // 3. 导入模型
      importProgressMessages.add('[任务] 正在导入大模型...');
      final modelResult = await _importModels();
      if (modelResult != null) {
        importError.value = modelResult;
        importProgressMessages.add('[错误] 大模型导入失败: $modelResult');
        return;
      }
      importProgressMessages.add('[完成] 大模型导入完成。');

      // 4. 导入工具
      importProgressMessages.add('[任务] 正在导入工具...');
      final toolResult = await _importTools();
      if (toolResult != null) {
        importError.value = toolResult;
        importProgressMessages.add('[错误] 工具导入失败: $toolResult');
        return;
      }
      importProgressMessages.add('[完成] 工具导入完成。');

      // 5. 导入智能体
      importProgressMessages.add('[任务] 正在导入智能体...');
      final agentResult = await _importAgents();
      if (agentResult != null) {
        importError.value = agentResult;
        importProgressMessages.add('[错误] 智能体导入失败: $agentResult');
        return;
      }
      importProgressMessages.add('[完成] 智能体导入完成。');

      final isToolPlainText = parsingService.isToolPlainText.value;
      final isModelPlainText = parsingService.isModelPlainText.value;
      if (!isModelPlainText && !isToolPlainText) {
        importProgressMessages.add('[注意] 外部工具和部分大模型 API Key 未包含在导入配置中。如需完全启用智能体的所有功能，请前往 设置 -> 模型管理 和 工具管理，手动补充您的授权密钥。');
      } else if (!isModelPlainText) {
        importProgressMessages.add('[注意] 部分大模型 API Key 未包含在导入配置中。如需完全启用智能体的所有功能，请前往 设置 -> 模型管理，手动补充您的授权密钥。');
      } else if (!isToolPlainText) {
        importProgressMessages.add('[注意] 外部工具 API Key 未包含在导入配置中。如需完全启用智能体的所有功能，请前往 设置 -> 工具管理，手动补充您的授权密钥。');
      }
    } catch (e) {
      Log.e('执行导入异常: $e');
      importError.value = '执行导入失败: $e';
      importProgressMessages.add('[错误] 执行导入失败: $e');
    } finally {
      isImporting.value = false;
    }
  }

  /// 验证所有数据
  Future<String?> _validateAllData() async {
    try {
      // 验证模型别名唯一性（仅验证操作为"新建"的模型）
      if (parsingService.parsedModels.isNotEmpty) {
        final modelsToValidate = parsingService.parsedModels.values.where((model) => model.operate == ImportOperate.operateNew).toList();
        if (modelsToValidate.isNotEmpty) {
          final modelAliasErrors = await ModelImportUtil.validateModelAliases(modelsToValidate);
          if (modelAliasErrors.isNotEmpty) {
            return '模型别名验证失败: ${modelAliasErrors.join('; ')}';
          }
        }
      }

      // 验证工具名称唯一性（仅验证操作为"新建"的工具）
      if (parsingService.parsedTools.isNotEmpty) {
        final toolsToValidate = parsingService.parsedTools.values.where((tool) => tool.operate == ImportOperate.operateNew).toList();
        if (toolsToValidate.isNotEmpty) {
          final toolNameErrors = await ToolImportUtil.validateToolNames(toolsToValidate);
          if (toolNameErrors.isNotEmpty) {
            return '工具名称验证失败: ${toolNameErrors.join('; ')}';
          }
        }
      }

      // 验证智能体名称唯一性（仅验证操作为"新建"的智能体）
      final allAgents = [
        if (parsingService.rootAgent.value != null) parsingService.rootAgent.value!,
        ...parsingService.parsedAgents.values,
      ];
      if (allAgents.isNotEmpty) {
        final agentsToValidate = allAgents.where((agent) => agent.operate == ImportOperate.operateNew).toList();
        if (agentsToValidate.isNotEmpty) {
          final agentNameErrors = await _validateAgentNames(agentsToValidate);
          if (agentNameErrors.isNotEmpty) {
            return '智能体名称验证失败: ${agentNameErrors.join('; ')}';
          }
        }
      }

      // 验证Agent引用关系

      for (final agent in allAgents) {
        // 验证Agent名称
        if (agent.name.isEmpty) {
          return 'Agent名称不能为空: ${agent.id}';
        }

        // 验证模型引用（仅记录警告）
        if (agent.llmModelId.isNotEmpty && !parsingService.parsedModels.containsKey(agent.llmModelId)) {
          Log.w('Agent ${agent.name} 引用的LLM模型 ${agent.llmModelId} 不存在，将跳过该引用');
        }
        if (agent.ttsModelId.isNotEmpty && !parsingService.parsedModels.containsKey(agent.ttsModelId)) {
          Log.w('Agent ${agent.name} 引用的TTS模型 ${agent.ttsModelId} 不存在，将跳过该引用');
        }
        if (agent.asrModelId.isNotEmpty && !parsingService.parsedModels.containsKey(agent.asrModelId)) {
          Log.w('Agent ${agent.name} 引用的ASR模型 ${agent.asrModelId} 不存在，将跳过该引用');
        }

        // 验证工具引用（仅记录警告）
        if (agent.toolFunctionList != null && agent.toolFunctionList!.isNotEmpty) {
          for (final func in agent.toolFunctionList!) {
            if (!parsingService.parsedTools.containsKey(func.toolId)) {
              Log.w('Agent ${agent.name} 引用的工具 ${func.toolId} 不存在，将跳过该引用');
            }
          }
        }

        // 验证子Agent引用（仅记录警告）
        if (agent.subAgentIds.isNotEmpty) {
          for (final childId in agent.subAgentIds) {
            final childExists = parsingService.parsedAgents.values.any((child) => child.id == childId);
            if (!childExists) {
              Log.w('Agent ${agent.name} 引用的子Agent $childId 不存在，将跳过该引用');
            }
          }
        }
      }

      return null;
    } catch (e) {
      Log.e('数据验证失败: $e');
      return '数据验证失败: $e';
    }
  }

  /// 验证智能体名称唯一性，返回名称错误
  Future<List<String>> _validateAgentNames(List<AgentDTO> agents) async {
    final nameErrors = <String>[];
    if (agents.isEmpty) return nameErrors;

    final existingAgents = (await agentRepository.getAgentListFromBox()).toList();
    final importNames = <String, String>{};

    for (final agent in agents) {
      final name = agent.name;

      // 检查与现有智能体的名称冲突
      if (!AgentValidator.isNameUnique(name, existingAgents)) {
        nameErrors.add("智能体 '$name' 的名称已存在于现有智能体中");
      } else if (importNames.containsKey(name)) {
        nameErrors.add("导入的智能体中有多个智能体使用了相同的名称 '$name'");
      } else {
        importNames[name] = name;
      }
    }

    return nameErrors;
  }

  /// 导入模型
  Future<String?> _importModels() async {
    try {
      // 导入普通模型到本地数据库
      final modelEntries = parsingService.parsedModels.entries.toList();
      final modelIds = _generateUniqueIds(modelEntries.length);

      for (int i = 0; i < modelEntries.length; i++) {
        final oldId = modelEntries[i].key;
        ModelDTO modelDTO = modelEntries[i].value;
        final newId = modelIds[i];

        importProgressMessages.add('[任务] 正在导入模型: ${modelDTO.alias}');
        final newModelDTO = await ModelImportUtil.importSingleModelForAgent(modelDTO, newId, modelDTO.operate);
        parsingService.parsedModels[oldId] = newModelDTO;
        importProgressMessages.add('[完成] 模型导入成功: ${modelDTO.alias}');
      }

      // 注意：embedding 模型已在知识库上传阶段由服务器处理，无需再次上传
      // 模型ID映射关系已在解析阶段从知识库上传返回的 modelMap 中更新

      return null;
    } catch (e) {
      Log.e('模型导入失败: $e');
      return '模型导入失败: $e';
    }
  }

  /// 导入知识库
  Future<String?> _importKnowledgeBases() async {
    try {
      final uploadResult = parsingService.knowledgeBaseUploadResult;
      if (uploadResult == null || uploadResult.token == null || uploadResult.token!.isEmpty) {
        return '知识库上传token不存在';
      }

      final response = await libraryRepository.saveImportData(uploadResult);

      var data = response?.data;
      if (response?.code != 200 || data is! Map<String, String>) {
        return response?.message;
      }

      parsingService.knowledgeBaseIdMap.clear();
      // 处理服务器返回的知识库ID映射关系 (oldId -> newId)
      parsingService.knowledgeBaseIdMap.value = data.map((key, value) => MapEntry(key, value.toString()));

      return null;
    } catch (e) {
      Log.e('知识库导入失败: $e');
      return '知识库导入失败: $e';
    }
  }

  /// 导入工具
  Future<String?> _importTools() async {
    try {
      final toolEntries = parsingService.parsedTools.entries.toList();
      final toolIds = _generateUniqueIds(toolEntries.length);

      for (int i = 0; i < toolEntries.length; i++) {
        final oldId = toolEntries[i].key;
        final toolDTO = toolEntries[i].value;
        final newId = toolIds[i];

        importProgressMessages.add('[任务] 正在导入工具: ${toolDTO.name}');
        final newToolDTO = await ToolImportUtil.importSingleToolForAgent(toolDTO, newId, toolDTO.operate);
        parsingService.parsedTools[oldId] = newToolDTO;
        importProgressMessages.add('[完成] 工具导入成功: ${toolDTO.name}');
      }

      return null;
    } catch (e) {
      Log.e('工具导入失败: $e');
      return '工具导入失败: $e';
    }
  }

  /// 导入Agent
  Future<String?> _importAgents() async {
    try {
      final allAgents = [
        if (parsingService.rootAgent.value != null) parsingService.rootAgent.value!,
        ...parsingService.parsedAgents.values,
      ];

      if (allAgents.isEmpty) {
        return '没有可导入的智能体';
      }

      final agentBatchIds = _generateUniqueIds(allAgents.length);
      final agentIdMap = _buildAgentIdMapping(allAgents, agentBatchIds);

      final agentsToSave = await _prepareAgentsForSave(allAgents, agentBatchIds, agentIdMap);

      importProgressMessages.add('[任务] 正在保存智能体到数据库...');
      await _saveAgentsToRepository(agentsToSave);
      importProgressMessages.add('[完成] 智能体保存成功。');

      eventBus.fire(AgentMessageEvent(message: EventBusMessage.updateList));
      eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateList));
      eventBus.fire(ToolMessageEvent(message: EventBusMessage.updateList));
      return null;
    } catch (e) {
      Log.e('Agent导入失败: $e');
      return 'Agent导入失败: $e';
    }
  }

  /// 生成唯一ID列表
  List<String> _generateUniqueIds(int count) {
    var ids = BatchIdGenerator.instance.generateBatchIds(count);
    final idSet = <String>{};
    final duplicateIndices = <int>[];

    for (int i = 0; i < ids.length; i++) {
      var id = ids[i];
      if (idSet.contains(id)) {
        duplicateIndices.add(i);
      } else {
        idSet.add(id);
      }
    }

    for (final idx in duplicateIndices) {
      var newId = BatchIdGenerator.instance.generateUniqueId();
      while (idSet.contains(newId)) {
        newId = BatchIdGenerator.instance.generateUniqueId();
      }
      idSet.add(newId);
      ids[idx] = newId;
    }

    return ids;
  }

  /// 构建Agent ID映射
  Map<String, String> _buildAgentIdMapping(List<AgentDTO> agents, List<String> newIds) {
    final agentIdMap = <String, String>{};
    for (int i = 0; i < agents.length; i++) {
      final agent = agents[i];
      final newId = agent.operate == ImportOperate.operateNew ? newIds[i] : agent.similarId;

      final oldId = agent.id;
      agentIdMap[oldId] = newId ?? newIds[i];
    }
    return agentIdMap;
  }

  /// 准备Agent数据用于保存
  Future<Map<String, AgentModel>> _prepareAgentsForSave(
      List<AgentDTO> allAgents, List<String> agentBatchIds, Map<String, String> agentIdMap) async {
    final agentsToSave = <String, AgentModel>{};

    for (int i = 0; i < allAgents.length; i++) {
      final oldId = allAgents[i].id;
      final dto = allAgents[i];
      if (dto.operate == ImportOperate.operateSkip) {
        importProgressMessages.add('[任务] 正在处理智能体: ${dto.name}');
        importProgressMessages.add('[完成] 智能体处理完成: ${dto.name}');
        continue;
      }
      final agent = dto.toModel();
      agent.id = agentIdMap[oldId] ?? agentBatchIds[i];
      final similarAgent = await AgentRepository().getData(dto.similarId ?? "");
      if (dto.operate == ImportOperate.operateNew) {
        // while (!await AgentValidator.isNameUniqueAsync(agent.name)) {
        //   agent.name += "_1";
        // }
        agent.createTime = DateTime.now().microsecondsSinceEpoch;
      } else {
        agent.createTime = similarAgent?.createTime ?? DateTime.now().microsecondsSinceEpoch;
      }

      importProgressMessages.add('[任务] 正在处理智能体: ${agent.name}');

      // 更新模型引用
      _updateModelReferences(agent);

      // 更新知识库引用
      _updateKnowledgeBaseReferences(agent);

      // 更新子Agent引用
      _updateChildAgentReferences(agent, agentIdMap);

      // 更新函数引用
      await _updateFunctionReferences(agent, oldId);

      agentsToSave[agent.id] = agent;
      importProgressMessages.add('[完成] 智能体处理完成: ${agent.name}');
    }

    return agentsToSave;
  }

  /// 更新知识库引用
  void _updateKnowledgeBaseReferences(AgentModel agent) {
    // 更新 libraryIds（知识库ID列表）
    if (agent.libraryIds != null && agent.libraryIds!.isNotEmpty) {
      agent.libraryIds =
          agent.libraryIds?.map((oldId) => _getKnowledgeBaseNewId(oldId)).where((e) => e != null && e.isNotEmpty).cast<String>().toList();
    }
  }

  /// 获取知识库新ID
  String? _getKnowledgeBaseNewId(String oldId) {
    final newId = parsingService.knowledgeBaseIdMap[oldId];
    if (newId != null && newId.isNotEmpty) {
      return newId;
    }
    return null;
  }

  /// 更新模型引用
  void _updateModelReferences(AgentModel agent) {
    // LLM模型引用
    if (agent.modelId.isNotEmpty) {
      final modelDTO = parsingService.parsedModels[agent.modelId];
      if (modelDTO != null) {
        agent.modelId = modelDTO.id;
      } else {
        agent.modelId = '';
      }
    }

    // TTS模型引用
    if (agent.ttsModelId?.isNotEmpty == true) {
      final modelDTO = parsingService.parsedModels[agent.ttsModelId!];
      if (modelDTO != null) {
        agent.ttsModelId = modelDTO.id;
      } else {
        agent.ttsModelId = '';
      }
    }

    // ASR模型引用
    if (agent.asrModelId?.isNotEmpty == true) {
      final modelDTO = parsingService.parsedModels[agent.asrModelId!];
      if (modelDTO != null) {
        agent.asrModelId = modelDTO.id;
      } else {
        agent.asrModelId = '';
      }
    }
  }

  /// 更新子Agent引用
  void _updateChildAgentReferences(AgentModel agent, Map<String, String> agentIdMap) {
    if (agent.childAgentIds != null) {
      final originalIds = List<String>.from(agent.childAgentIds!);
      agent.childAgentIds = agent.childAgentIds?.map((oldChildId) => agentIdMap[oldChildId] ?? '').where((e) => e.isNotEmpty).toList();

      // 检查是否有子Agent引用被移除
      final removedIds = originalIds.where((id) => !agentIdMap.containsKey(id)).toList();
      if (removedIds.isNotEmpty) {
        Log.w('Agent ${agent.name} 的子Agent引用 ${removedIds.join(', ')} 不存在，已移除这些引用');
      }
    }
  }

  /// 更新函数引用
  Future<void> _updateFunctionReferences(AgentModel agent, String oldId) async {
    if (parsingService.functionRefs.containsKey(oldId)) {
      var funcRefs = parsingService.functionRefs[oldId];
      if (funcRefs != null) {
        agent.functionList ??= [];
        for (var functionRef in funcRefs) {
          final dto = parsingService.parsedTools[functionRef.toolId];
          if (dto != null) {
            if (dto.schemaType == ToolValidator.DTO_OPEN_TOOL_SERVER || dto.schemaType == ToolValidator.DTO_MCP) {
              if (agent.functionList?.any((element) => element.toolId == dto.id) == true) {
                continue;
              }
              ToolFunctionModel function = ToolFunctionModel()
                ..toolId = dto.id
                ..toolName = dto.name;
              agent.functionList?.add(function);
              continue;
            }
            var toolModel = dto.toModel();
            await toolModel.initFunctions();
            for (var function in toolModel.functionList) {
              var functionId = ToolParser.generateFunctionIdByFunction(function);
              if (functionRef.functionId == functionId) {
                agent.functionList?.add(function);
              }
            }
          }
        }
      }
    }
  }

  /// 保存Agent到仓库
  Future<void> _saveAgentsToRepository(Map<String, AgentModel> agentsToSave) async {
    if (agentsToSave.isNotEmpty) {
      await agentRepository.updateAgents(agentsToSave);
    }
  }

  /// 更新上传数据，将用户在UI中修改的模型和知识库数据同步到上传结果中
  void _updateUploadData() {
    final uploadResult = parsingService.knowledgeBaseUploadResult;
    if (uploadResult == null) {
      return;
    }

    // 更新modelMap：同步所有更新的字段
    final modelMap = uploadResult.modelMap;
    if (modelMap != null) {
      // 更新knowledgeBaseModels中的模型数据
      for (final entry in modelMap.entries) {
        final modelId = entry.key;
        final uploadModelData = entry.value;
        final modelData = parsingService.knowledgeBaseModels[modelId];
        if (modelData != null) {
          _updateModelMapFromModel(uploadModelData, modelData);
        }
      }
    }

    // 更新knowledgeBaseMap：只更新operate字段
    final knowledgeBaseMap = uploadResult.knowledgeBaseMap;
    if (knowledgeBaseMap != null) {
      for (final entry in knowledgeBaseMap.entries) {
        final kbId = entry.key;
        final uploadKbData = entry.value;
        final kbData = parsingService.parsedKnowledgeBases[kbId];
        if (kbData != null) {
          uploadKbData['operate'] = kbData.operate;
        }
      }
    }
  }

  /// 从ModelDTO更新modelMap中的数据
  void _updateModelMapFromModel(Map<String, dynamic> uploadModelData, ModelDTO model) {
    uploadModelData['alias'] = model.alias;
    uploadModelData['name'] = model.name;
    uploadModelData['baseUrl'] = model.baseUrl;
    uploadModelData['apiKey'] = model.apiKey;
    uploadModelData['maxTokens'] = model.maxTokens;
    uploadModelData['autoAgent'] = model.autoAgent;
    uploadModelData['toolInvoke'] = model.toolInvoke;
    uploadModelData['deepThink'] = model.deepThink;
    uploadModelData['operate'] = model.operate;
  }
}
