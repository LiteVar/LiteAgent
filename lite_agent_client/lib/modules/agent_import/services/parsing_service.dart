import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/models/local/library_upload_result.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/agent/agent_converter.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/model/model_import.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';
import 'package:lite_agent_client/utils/tool/tool_import.dart';
import 'package:path_provider/path_provider.dart';

import '../../../config/constants.dart';
import '../../../repositories/agent_repository.dart';
import '../../../utils/agent/agent_validator.dart';
import '../../../utils/tool/tool_validator.dart';
import 'file_service.dart';

class ParsingService extends GetxService {
  final fileService = Get.find<FileService>();

  final parsedModels = <String, ModelDTO>{}.obs;
  final knowledgeBaseModels = <String, ModelDTO>{}.obs;
  final parsedTools = <String, ToolDTO>{}.obs;
  final parsedKnowledgeBases = <String, LibraryDto>{}.obs;
  final parsedAgents = <String, AgentDTO>{}.obs;
  final rootAgent = Rx<AgentDTO?>(null);
  final rootAgentFileNameId = RxnString();

  LibraryUploadResult? knowledgeBaseUploadResult;
  final knowledgeBaseIdMap = <String, String>{}.obs;
  final functionRefs = <String, List<_FuncRef>>{}.obs;

  final parsingProgressMessages = <String>[].obs;

  final isModelPlainText = false.obs;
  final isToolPlainText = false.obs;
  final isKnowledgeBasePlainText = false.obs;

  final isParsingModels = false.obs;
  final modelParseError = RxnString();
  final isParsingTools = false.obs;
  final toolParseError = RxnString();
  final isParsingKnowledgeBases = false.obs;
  final knowledgeBaseParseError = RxnString();
  final isParsingAgents = false.obs;
  final agentParseError = RxnString();

  String _lastParsedFilePath = '';

  Future<void> startParsing() async {
    if (fileService.selectedFile.value == null) {
      _showErrorDialog('请先选择文件');
      return;
    }

    final currentFilePath = fileService.selectedFile.value!.path;
    // 如果是同一个文件且已成功解析（没有错误），则跳过重复解析
    if (_shouldSkipParsing(currentFilePath)) {
      Log.d('跳过重复解析: $currentFilePath');
      return;
    }

    try {
      _clearAllParsedData();
      _clearAllErrors();

      isParsingKnowledgeBases.value = true;

      parsingProgressMessages.add('[任务] 正在解压文件...');
      final tempDir = await _unpackFile();
      if (tempDir == null) return;

      parsingProgressMessages.add('[任务] 正在加载知识库配置文件...');
      final knowledgeBaseModelIds = await _analyzeKnowledgeBasesForModels();
      if (_hasError(knowledgeBaseParseError)) return;
      parsingProgressMessages.add('[完成] 知识库模型解析完成。');

      _switchParsingState(isParsingKnowledgeBases, isParsingModels);
      parsingProgressMessages.add('[任务] 正在加载大模型配置文件...');
      await _parseModels(knowledgeBaseModelIds);
      if (_hasError(modelParseError, isParsingModels)) return;
      parsingProgressMessages.add('[完成] 大模型配置解析完成。');

      _switchParsingState(isParsingModels, isParsingTools);
      parsingProgressMessages.add('[任务] 正在加载工具配置文件...');
      await _parseTools();
      if (_hasError(toolParseError, isParsingTools)) return;
      parsingProgressMessages.add('[完成] 所有工具配置解析完成。');

      _switchParsingState(isParsingTools, isParsingKnowledgeBases);
      parsingProgressMessages.add('[任务] 正在解析知识库...');
      await _processKnowledgeBases();
      if (_hasError(knowledgeBaseParseError, isParsingKnowledgeBases)) return;
      parsingProgressMessages.add('[完成] 所有知识库配置解析完成。');

      _switchParsingState(isParsingKnowledgeBases, isParsingAgents);
      parsingProgressMessages.add('[任务] 正在加载智能体配置文件...');
      await _parseAgents();
      if (_hasError(agentParseError, isParsingAgents)) return;
      parsingProgressMessages.add('[完成] 所有智能体配置解析完成。');

      isParsingAgents.value = false;
      _lastParsedFilePath = currentFilePath;
    } catch (e) {
      _handleParsingException(e);
    }
  }

  bool _shouldSkipParsing(String filePath) {
    // 只有在同一个文件、有进度消息、且没有错误的情况下才跳过解析
    // 如果有错误，说明上次解析失败，应该允许重试
    final hasNoErrors = modelParseError.value == null &&
        toolParseError.value == null &&
        knowledgeBaseParseError.value == null &&
        agentParseError.value == null;
    return _lastParsedFilePath == filePath && parsingProgressMessages.isNotEmpty && hasNoErrors;
  }

  void _clearAllParsedData() {
    parsingProgressMessages.clear();
    isModelPlainText.value = false;
    isToolPlainText.value = false;
    isKnowledgeBasePlainText.value = false;
    parsedModels.clear();
    knowledgeBaseModels.clear();
    parsedTools.clear();
    parsedKnowledgeBases.clear();
    parsedAgents.clear();
    rootAgent.value = null;
  }

  void _clearAllErrors() {
    modelParseError.value = null;
    toolParseError.value = null;
    knowledgeBaseParseError.value = null;
    agentParseError.value = null;
  }

  Future<Directory?> _unpackFile() async {
    final tempDir = await fileService.unpackZipToTemp(fileService.selectedFile.value!);
    if (tempDir == null) {
      knowledgeBaseParseError.value = '文件解压失败';
      isParsingKnowledgeBases.value = false;
    }
    return tempDir;
  }

  bool _hasError(RxnString errorField, [RxBool? stateToReset]) {
    if (errorField.value != null) {
      stateToReset?.value = false;
      return true;
    }
    return false;
  }

  void _switchParsingState(RxBool currentState, RxBool nextState) {
    currentState.value = false;
    nextState.value = true;
  }

  void _handleParsingException(Object e) {
    modelParseError.value = '文件处理失败: ${e.toString()}';
    isParsingModels.value = false;
    isParsingTools.value = false;
    isParsingKnowledgeBases.value = false;
    isParsingAgents.value = false;
    fileService.cleanupTempDir();
  }

  String _getFileNameWithoutExtension(String path) {
    return path.replaceAll('\\', '/').split('/').last.replaceAll('.json', '');
  }

  /// 解析模型
  Future<void> _parseModels(Set<String> knowledgeBaseModelIds) async {
    final tempDir = fileService.tempDir;
    if (tempDir == null) {
      Log.e('解析模型失败: tempDir为null');
      return;
    }

    try {
      final existingModels = (await modelRepository.getModelListFromBox()).toList();
      final modelsDir = Directory('${tempDir.path}/models');
      if (!modelsDir.existsSync()) return;

      final modelFiles = modelsDir.listSync().whereType<File>().where((f) => f.path.toLowerCase().endsWith('.json'));

      for (final file in modelFiles) {
        try {
          final result = await ModelImportUtil.validateSingleModelFile(file, enableEmbeddingModel: true);
          final model = result.model;
          if (model == null) {
            modelParseError.value = '解析模型文件失败: ${result.error}';
            return;
          }
          final originalId = _getFileNameWithoutExtension(file.path);

          if (model.baseUrl != "{{<ENDPOINT>}}" && model.apiKey != "{{<APIKEY>}}") {
            isModelPlainText.value = true;
          }

          if (knowledgeBaseModelIds.contains(originalId)) {
            knowledgeBaseModels[originalId] = model;
            parsingProgressMessages.add('[完成] 加载知识库模型: ${model.alias} (将上传到服务器)');
          } else {
            model.similarId = ModelValidator.getSameAliasModelId(model.alias, existingModels) ?? "";
            model.operate = ImportOperate.operateNew;
            parsedModels[originalId] = model;
            parsingProgressMessages.add('[完成] 加载大模型: ${model.alias}');
          }
        } catch (e) {
          Log.e('解析模型文件失败: ${file.path}, error=$e');
          modelParseError.value = '解析模型文件失败: ${e.toString()}';
          return;
        }
      }
    } catch (e) {
      Log.e('解析模型失败: $e');
      modelParseError.value = '解析模型失败: ${e.toString()}';
    }
  }

  Future<void> _parseTools() async {
    final tempDir = fileService.tempDir;
    if (tempDir == null) {
      Log.e('解析工具失败: tempDir为null');
      return;
    }

    try {
      final existingTools = (await toolRepository.getToolListFromBox()).toList();
      parsedTools.clear();

      final toolsDir = Directory('${tempDir.path}/tools');
      if (!toolsDir.existsSync()) return;

      final toolFiles = toolsDir.listSync().whereType<File>().where((f) => f.path.toLowerCase().endsWith('.json'));

      for (final file in toolFiles) {
        try {
          final result = await ToolImportUtil.validateSingleFile(file);
          final tool = result.toolDTO;
          if (tool == null) {
            toolParseError.value = '解析工具文件失败: ${result.error}';
            return;
          }

          if (tool.apiKey != "{{<APIKEY>}}") {
            isToolPlainText.value = true;
          }

          tool.similarId = ToolValidator.getSameNameToolId(tool.name, existingTools) ?? "";
          tool.operate = ImportOperate.operateNew;

          final originalId = _getFileNameWithoutExtension(file.path);
          parsedTools[originalId] = tool;
          parsingProgressMessages.add('[完成] 加载工具: ${tool.name}');
        } catch (e) {
          Log.e('解析工具文件失败: ${file.path}, error=$e');
          toolParseError.value = '解析工具文件失败: ${e.toString()}';
          return;
        }
      }
    } catch (e) {
      Log.e('解析工具失败: $e');
      toolParseError.value = '解析工具失败: ${e.toString()}';
    }
  }

  Future<Set<String>> _analyzeKnowledgeBasesForModels() async {
    final tempDir = fileService.tempDir;
    final modelIds = <String>{};
    if (tempDir == null) {
      Log.e('分析知识库失败: tempDir为null');
      return modelIds;
    }

    try {
      final knowledgeBasesDir = Directory('${tempDir.path}/knowledge_bases');
      if (!knowledgeBasesDir.existsSync()) return modelIds;

      final kbDirs = knowledgeBasesDir.listSync().whereType<Directory>();
      for (final kbDir in kbDirs) {
        final metadataFile = File('${kbDir.path}/metadata.json');
        if (!metadataFile.existsSync()) continue;

        try {
          final metadata = json.decode(await metadataFile.readAsString()) as Map<String, dynamic>;
          final kbName = metadata['name'] as String? ?? kbDir.path.replaceAll('\\', '/').split('/').last;

          final embeddingModelId = metadata['embeddingModelId'] as String?;
          if (embeddingModelId?.isNotEmpty ?? false) {
            modelIds.add(embeddingModelId!);
            Log.i('知识库[$kbName] 引用 embedding 模型: $embeddingModelId');
          }

          final llmModelId = metadata['llmModelId'] as String?;
          if (llmModelId?.isNotEmpty ?? false) {
            modelIds.add(llmModelId!);
            Log.i('知识库[$kbName] 引用 llm 模型: $llmModelId');
          }
        } catch (e) {
          Log.w('读取知识库 metadata 失败: ${metadataFile.path}, error=$e');
        }
      }

      Log.i('知识库模型分析完成: 共 ${modelIds.length} 个模型ID, 模型列表: ${modelIds.join(", ")}');
    } catch (e) {
      Log.e('分析知识库失败: $e');
      knowledgeBaseParseError.value = '分析知识库失败: ${e.toString()}';
    }
    return modelIds;
  }

  Future<void> _processKnowledgeBases() async {
    final tempDir = fileService.tempDir;
    if (tempDir == null) {
      Log.e('解析知识库失败: tempDir为null');
      return;
    }

    try {
      parsedKnowledgeBases.clear();
      final knowledgeBasesDir = Directory('${tempDir.path}/knowledge_bases');
      if (!knowledgeBasesDir.existsSync()) return;

      if (!await accountRepository.isLogin()) {
        knowledgeBaseParseError.value = '未登录无法继续导入知识库。请登录后重试。';
        return;
      }

      parsingProgressMessages.add('[任务] 正在打包知识库和模型文件...');
      final zipFilePath = await _compressKnowledgeBasesAndModels(knowledgeBasesDir);
      if (zipFilePath == null) {
        knowledgeBaseParseError.value = '压缩知识库文件夹失败';
        return;
      }

      parsingProgressMessages.add('[任务] 正在上传知识库文件到服务器...');
      try {
        final uploadResult = await libraryRepository.uploadLibraryZip(zipFilePath);
        if (uploadResult == null) {
          Log.e('上传知识库失败');
          knowledgeBaseParseError.value = '知识库上传失败';
          return;
        }

        _handleUploadSuccess(uploadResult);
        parsingProgressMessages.add('[完成] 知识库文件上传完成。');
      } catch (e) {
        Log.e('上传知识库失败: $e');
        knowledgeBaseParseError.value = '上传知识库失败: ${e.toString()}';
      } finally {
        _cleanupZipFile(zipFilePath);
      }
    } catch (e) {
      Log.e('解析知识库失败: $e');
      knowledgeBaseParseError.value = '解析知识库失败: ${e.toString()}';
    }
  }

  void _handleUploadSuccess(LibraryUploadResult result) {
    final token = result.token;
    if (token?.isNotEmpty ?? false) {
      knowledgeBaseUploadResult = result;
    }

    parsedKnowledgeBases.clear();
    result.knowledgeBaseMap?.forEach((kbId, kbData) {
      parsedKnowledgeBases[kbId] = _createLibraryDtoFromData(kbId, kbData as Map<String, dynamic>);
    });

    knowledgeBaseModels.clear();
    result.modelMap?.forEach((key, value) {
      var model = ModelDTO.fromJson(value);

      if (model.apiKey != "{{<APIKEY>}}" && model.baseUrl != "{{<ENDPOINT>}}") {
        isKnowledgeBasePlainText.value = true;
      }

      knowledgeBaseModels[key] = model;
    });
  }

  void _cleanupZipFile(String zipFilePath) {
    try {
      final zipFile = File(zipFilePath);
      if (zipFile.existsSync()) zipFile.deleteSync();
    } catch (e) {
      Log.w('清理临时压缩文件失败: $e');
    }
  }

  Future<String?> _compressKnowledgeBasesAndModels(Directory knowledgeBasesDir) async {
    final tempDir = fileService.tempDir;
    if (tempDir == null) return null;

    try {
      final archive = Archive();
      _addKnowledgeBaseFolderToArchive(archive, knowledgeBasesDir);
      _addKnowledgeBaseModelsToArchive(archive, tempDir);

      final zipData = ZipEncoder().encode(archive);
      final systemTempDir = await getTemporaryDirectory();
      final zipFilePath = '${systemTempDir.path}/knowledge_bases_${DateTime.now().microsecondsSinceEpoch}.zip';

      final zipFile = File(zipFilePath);
      await zipFile.writeAsBytes(zipData);

      final fileSizeKB = ((await zipFile.length()) / 1024).toStringAsFixed(2);
      Log.d('知识库文件夹压缩完成: $zipFilePath, 文件大小: ${fileSizeKB}KB');
      return zipFilePath;
    } catch (e) {
      Log.e('压缩知识库文件夹失败: $e');
      return null;
    }
  }

  void _addKnowledgeBaseFolderToArchive(Archive archive, Directory knowledgeBasesDir) {
    final folderName = knowledgeBasesDir.path.replaceAll('\\', '/').split('/').last;
    final entities = knowledgeBasesDir.listSync(recursive: true);

    for (final entity in entities) {
      if (entity is! File) continue;

      final normalizedKbPath = knowledgeBasesDir.path.replaceAll('\\', '/');
      final normalizedEntityPath = entity.path.replaceAll('\\', '/');
      final fileRelativePath = normalizedEntityPath.replaceFirst('$normalizedKbPath/', '');
      final relativePath = '$folderName/$fileRelativePath';
      final bytes = entity.readAsBytesSync();
      archive.addFile(ArchiveFile(relativePath, bytes.length, bytes));
    }
  }

  void _addKnowledgeBaseModelsToArchive(Archive archive, Directory tempDir) {
    final modelsDir = Directory('${tempDir.path}/models');

    for (final modelId in knowledgeBaseModels.keys) {
      final modelFile = File('${modelsDir.path}/$modelId.json');
      if (!modelFile.existsSync()) continue;

      final relativePath = 'models/$modelId.json';
      final bytes = modelFile.readAsBytesSync();
      archive.addFile(ArchiveFile(relativePath, bytes.length, bytes));
      Log.d('添加知识库模型到压缩包: $relativePath');
    }
  }

  Future<void> _parseAgents() async {
    final tempDir = fileService.tempDir;
    if (tempDir == null) {
      Log.e('解析智能体失败: tempDir为null');
      return;
    }

    try {
      final existingAgents = (await agentRepository.getAgentListFromBox()).toList();
      parsedAgents.clear();

      await _parseRootAgent(tempDir, existingAgents);
      if (rootAgent.value == null) {
        agentParseError.value = '未找到有效的根智能体配置文件';
        return;
      }

      await _parseChildAgents(tempDir, existingAgents);
    } catch (e) {
      Log.e('解析智能体失败: $e');
      agentParseError.value = '解析智能体失败: ${e.toString()}';
    }
  }

  Future<void> _parseRootAgent(Directory tempDir, List<AgentModel> existingAgents) async {
    final rootJsonFiles = tempDir.listSync().whereType<File>().where((f) => f.path.toLowerCase().endsWith('.json'));

    for (final file in rootJsonFiles) {
      final fileName = file.path.replaceAll('\\', '/').split('/').last.toLowerCase();
      if (fileName == 'metadata.json' || fileName == 'package.json') continue;

      try {
        final agent = await _parseSingleAgentFile(file, existingAgents);
        if (agent == null) continue;

        rootAgentFileNameId.value = _getFileNameWithoutExtension(file.path);
        rootAgent.value = agent;

        Log.d('加载根智能体: ${agent.name}');
        return;
      } catch (e) {
        Log.w('解析根智能体文件失败: ${file.path}, error=$e');
      }
    }
  }

  Future<void> _parseChildAgents(Directory tempDir, List<AgentModel> existingAgents) async {
    final multiagentDir = Directory('${tempDir.path}/multiagent');
    if (!multiagentDir.existsSync()) return;

    final agentFiles = multiagentDir.listSync().where((f) => f.path.endsWith('.json')).toList();

    for (final file in agentFiles) {
      try {
        final agent = await _parseSingleAgentFile(File(file.path), existingAgents);
        if (agent == null) continue;

        final originalId = _getFileNameWithoutExtension(file.path);
        parsedAgents[originalId] = agent;
        Log.d('加载子智能体: ${agent.name}');
      } catch (e) {
        Log.e('解析子智能体文件失败: ${file.path}, error=$e');
        agentParseError.value = '解析子智能体文件失败: ${e.toString()}';
        return;
      }
    }
  }

  Future<AgentDTO?> _parseSingleAgentFile(File file, List<AgentModel> existingAgents) async {
    try {
      final map = json.decode(await file.readAsString()) as Map<String, dynamic>;
      if (!map.containsKey('name') || !map.containsKey('id')) return null;

      _normalizeAgentMap(map);
      final agent = AgentDTO.fromJson(map);
      agent.similarId = AgentValidator.getSameNameAgentId(agent.name, existingAgents) ?? "";
      agent.operate = ImportOperate.operateNew;

      final functionList = (map['functionList'] as List<dynamic>?) ?? const [];
      functionRefs[agent.id] = _parseFunctionReferences(functionList);

      Log.d('子Agent函数引用: ${functionRefs[agent.id]?.length ?? 0}个');
      return agent;
    } catch (e) {
      Log.e('解析智能体文件失败: ${file.path}, error=$e');
      rethrow;
    }
  }

  void _normalizeAgentMap(Map<String, dynamic> map) {
    if (!map.containsKey('llmModelId') && map.containsKey('modelId')) {
      map['llmModelId'] = map['modelId'];
    }
    if (!map.containsKey('datasetIds') && map.containsKey('knowledgeBaseIds')) {
      map['datasetIds'] = map['knowledgeBaseIds'];
    }
    if (map.containsKey('type')) {
      map['type'] = AgentConverter.stringToType(map['type']?.toString());
    }
    if (map.containsKey('mode')) {
      map['mode'] = AgentConverter.stringToMode(map['mode']?.toString());
    }
  }

  List<_FuncRef> _parseFunctionReferences(List<dynamic> functionList) {
    return functionList.map((func) {
      final funcMap = func as Map<String, dynamic>;
      return _FuncRef(
        funcMap['toolId'] as String? ?? '',
        funcMap['functionId'] as String? ?? '',
        funcMap['mode'] as String? ?? '',
      );
    }).toList();
  }

  void _showErrorDialog(String message) {
    Get.dialog(
      AlertDialog(
        title: const Text('错误'),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Get.back(),
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  void reset() {
    parsedModels.clear();
    knowledgeBaseModels.clear();
    parsedTools.clear();
    parsedKnowledgeBases.clear();
    knowledgeBaseUploadResult = null;
    knowledgeBaseIdMap.clear();
    parsedAgents.clear();
    rootAgent.value = null;
    rootAgentFileNameId.value = null;
    parsingProgressMessages.clear();
    isParsingModels.value = false;
    modelParseError.value = null;
    isParsingTools.value = false;
    toolParseError.value = null;
    isParsingKnowledgeBases.value = false;
    knowledgeBaseParseError.value = null;
    isParsingAgents.value = false;
    agentParseError.value = null;
    _lastParsedFilePath = '';
    functionRefs.clear();
  }

  LibraryDto _createLibraryDtoFromData(String kbId, Map<String, dynamic> kbData) {
    final metadata = kbData['metadata'] as Map<String, dynamic>? ?? {};
    final documents = kbData['documents'] as Map<String, dynamic>? ?? {};

    String name = metadata['name'] as String? ?? '';
    String description = metadata['description'] as String? ?? '';
    String embeddingModelId = metadata['embeddingModelId'] as String? ?? '';
    String llmModelId = metadata['llmModelId'] as String? ?? '';
    String similarId = kbData['similarId'] as String? ?? '';
    int operate = kbData['operate'] as int? ?? ImportOperate.operateNew;

    return LibraryDto(kbId, name, '', description, false, '', documents.length, 0, 0, embeddingModelId, llmModelId, similarId, operate);
  }
}

class _FuncRef {
  final String toolId;
  final String functionId;
  final String mode;

  _FuncRef(this.toolId, this.functionId, this.mode);
}
