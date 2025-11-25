import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:lite_agent_client/utils/extension/agent_extension.dart';
import 'package:lite_agent_client/utils/extension/tool_extension.dart';
import 'package:path_provider/path_provider.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/agent/agent_converter.dart';
import 'package:lite_agent_client/utils/model/model_converter.dart';
import 'package:lite_agent_client/utils/model/model_export.dart';
import 'package:lite_agent_client/utils/tool/tool_export.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';

import '../tool/tool_function_parser.dart';

class AgentExportUtil {
  static Future<String?> exportAgentToZip(AgentDTO agentDTO, {bool exportPlaintext = false}) async {
    try {
      var savePath = await FilePicker.platform
          .saveFile(dialogTitle: '选择保存位置', fileName: agentDTO.name, type: FileType.custom, allowedExtensions: ['agent']);

      // 如果用户取消选择，直接返回
      if (savePath == null) {
        return null;
      }

      // 统一确保只有一个 .agent 扩展名
      if (!savePath.toLowerCase().endsWith('.agent')) {
        savePath = '$savePath.agent';
      }

      // 显示加载框
      EasyLoading.show(status: '导出中...');

      try {
        final warningMessage = await _createAgentZip(agentDTO, savePath, exportPlaintext: exportPlaintext);
        Log.i("Agent数据导出成功: $savePath");
        
        // 如果有警告信息，显示给用户
        if (warningMessage != null && warningMessage.isNotEmpty) {
          EasyLoading.dismiss();
          AlarmUtil.showAlertDialog(warningMessage);
        }
        
        return savePath;
      } catch (e) {
        Log.e("导出Agent数据失败", e);
        return null;
      } finally {
        EasyLoading.dismiss();
      }
    } catch (e) {
      Log.e("导出Agent数据失败", e);
      EasyLoading.dismiss();
      return null;
    }
  }

  static Future<Map<String, dynamic>> _buildExportData(AgentDTO agentDTO,
      {Set<String>? successfulDatasetIds, Map<String, ToolModel>? toolMap}) async {
    var functionList = <Map<String, dynamic>>[];
    if (agentDTO.toolFunctionList != null) {
      for (var function in agentDTO.toolFunctionList!) {
        if (toolMap != null && function.toolId.isNotEmpty) {
          final tool = toolMap[function.toolId];
          if (tool != null && tool.schemaType == ToolValidator.OPTION_OPENTOOL_SERVER) {
            await tool.initFunctions();
            for (var func in tool.functionList) {
              final functionId = ToolParser.generateFunctionIdByFunction(func);
              final modeString = AgentConverter.modeToString(function.mode);
              var functionData = {"toolId": function.toolId, "functionId": functionId, "mode": modeString};
              functionList.add(functionData);
            }
            continue;
          }
        }

        // 普通处理：使用原始的 function
        final functionId = ToolParser.generateFunctionIdByFunctionDTO(function);
        final modeString = AgentConverter.modeToString(function.mode);
        var functionData = {"toolId": function.toolId, "functionId": functionId, "mode": modeString};
        functionList.add(functionData);
      }
    }

    // 过滤出成功导出的 datasetIds
    List<String>? knowledgeBaseIds;
    if (agentDTO.datasetIds != null) {
      if (successfulDatasetIds != null) {
        knowledgeBaseIds = agentDTO.datasetIds!.where((id) => successfulDatasetIds.contains(id)).toList();
      } else {
        knowledgeBaseIds = agentDTO.datasetIds;
      }
    }

    return {
      "id": agentDTO.id,
      "name": agentDTO.name,
      "description": agentDTO.description,
      "prompt": agentDTO.prompt,
      "type": AgentConverter.typeToString(agentDTO.type),
      "mode": AgentConverter.modeToString(agentDTO.mode),
      "modelId": agentDTO.llmModelId,
      "temperature": agentDTO.temperature,
      "topP": agentDTO.topP,
      "maxTokens": agentDTO.maxTokens,
      "ttsModelId": agentDTO.ttsModelId,
      "asrModelId": agentDTO.asrModelId,
      "functionList": functionList,
      "subAgentIds": agentDTO.subAgentIds,
      "knowledgeBaseIds": knowledgeBaseIds,
    };
  }

  static Future<String?> _createAgentZip(AgentDTO agentDTO, String savePath, {bool exportPlaintext = false}) async {
    final tempWorkspace = await _createTempWorkspace();
    final tempPath = tempWorkspace.path;
    String? warningMessage;

    try {
      final metadataJsonFile = await _writeMetadataJson(tempPath);
      final paths = _buildExportPaths(tempPath);

      final baseIds = _collectBaseDependencies(agentDTO);
      final collected = await _exportSubAgentsAndCollectDependencies(agentDTO, paths.multiagentDirPath, successfulDatasetIds: null);

      final allModelIds = {...baseIds.modelIds, ...collected.modelIds};
      final allToolIds = {...baseIds.toolIds, ...collected.toolIds};
      final allDatasetIds = {...baseIds.datasetIds, ...collected.datasetIds};

      // 批量读取所有需要的 tools，避免重复读取 Hive
      final toolMap = <String, ToolModel>{};
      for (final toolId in allToolIds) {
        final tool = await toolRepository.getToolFromBox(toolId);
        if (tool != null) {
          toolMap[toolId] = tool;
        }
      }

      await _exportModels(allModelIds, paths.modelsDirPath, exportPlaintext: exportPlaintext);
      await _exportTools(allToolIds, paths.toolsDirPath, exportPlaintext: exportPlaintext, toolMap: toolMap);

      // 未登录则跳过知识库导出
      final successfulDatasetIds = <String>{};
      if (await accountRepository.isLogin()) {
        final result =
            await _exportKnowledgeBases(allDatasetIds, paths.knowledgeBasesDirPath, paths.modelsDirPath, exportPlaintext: exportPlaintext);
        successfulDatasetIds.addAll(result.successfulIds);

        // 只有在发生错误时才设置警告信息
        if (result.hasError && allDatasetIds.isNotEmpty) {
          warningMessage = '知识库导出失败，Agent已导出但不包含知识库数据';
          Log.w(warningMessage);
        }
      }

      // 导出主 agent 和子 agent 的 JSON 文件，使用成功的 datasetIds
      final agentJsonFile = await _writeAgentJson(agentDTO, tempPath, successfulDatasetIds: successfulDatasetIds, toolMap: toolMap);
      await _rewriteSubAgentsJson(agentDTO, paths.multiagentDirPath, successfulDatasetIds, toolMap: toolMap);

      final archive = Archive();
      await _addFileToArchive(archive, agentJsonFile, tempPath);
      await _addFileToArchive(archive, metadataJsonFile, tempPath);
      await _addDirectoryToArchiveIfExists(archive, tempPath, paths.modelsDirPath);
      await _addDirectoryToArchiveIfExists(archive, tempPath, paths.toolsDirPath);
      await _addDirectoryToArchiveIfExists(archive, tempPath, paths.knowledgeBasesDirPath);
      await _addDirectoryToArchiveIfExists(archive, tempPath, paths.multiagentDirPath);

      final zipBytes = ZipEncoder().encode(archive);
      final zipFile = File(savePath);
      await zipFile.writeAsBytes(zipBytes);
      
      return warningMessage;
    } finally {
      await tempWorkspace.delete(recursive: true);
    }
  }

  static Future<Directory> _createTempWorkspace() async {
    final tempDir = await getTemporaryDirectory();
    final tempPath = '${tempDir.path}/agent_export_${DateTime.now().microsecondsSinceEpoch}';
    final tempDirFile = Directory(tempPath);
    await tempDirFile.create(recursive: true);
    return tempDirFile;
  }

  static Future<File> _writeAgentJson(AgentDTO agentDTO, String tempPath,
      {Set<String>? successfulDatasetIds, Map<String, ToolModel>? toolMap}) async {
    final exportData = await _buildExportData(agentDTO, successfulDatasetIds: successfulDatasetIds, toolMap: toolMap);
    final fileName = _generateFileName(agentDTO);
    final agentJsonPath = '$tempPath/$fileName';
    final agentJsonFile = File(agentJsonPath);
    await agentJsonFile.writeAsString(const JsonEncoder.withIndent('  ').convert(exportData), encoding: utf8);
    return agentJsonFile;
  }

  static Future<({Set<String> modelIds, Set<String> toolIds, Set<String> datasetIds})> _exportSubAgentsAndCollectDependencies(
      AgentDTO root, String multiagentDirPath,
      {Set<String>? successfulDatasetIds, Map<String, ToolModel>? toolMap}) async {
    final modelIds = <String>{};
    final toolIds = <String>{};
    final datasetIds = <String>{};

    final visited = <String>{};
    final queue = <String>[];
    queue.addAll(root.subAgentIds);

    var created = false;
    while (queue.isNotEmpty) {
      final id = queue.removeAt(0);
      if (id.isEmpty || visited.contains(id)) continue;
      final sub = await agentRepository.getAgentFromBox(id);
      if (sub == null) continue;
      visited.add(id);

      final dto = sub.toDTO();
      final data = await _buildExportData(dto, successfulDatasetIds: successfulDatasetIds, toolMap: toolMap);
      if (!created) {
        await Directory(multiagentDirPath).create();
        created = true;
      }
      final path = '$multiagentDirPath/${sub.id}.json';
      await File(path).writeAsString(const JsonEncoder.withIndent('  ').convert(data), encoding: utf8);

      if (dto.llmModelId.isNotEmpty) modelIds.add(dto.llmModelId);
      if (dto.ttsModelId.isNotEmpty) modelIds.add(dto.ttsModelId);
      if (dto.asrModelId.isNotEmpty) modelIds.add(dto.asrModelId);

      if (dto.toolFunctionList != null) {
        for (final f in dto.toolFunctionList!) {
          if (f.toolId.isNotEmpty) toolIds.add(f.toolId);
        }
      }

      if (dto.datasetIds?.isNotEmpty == true) {
        datasetIds.addAll(dto.datasetIds!);
      }

      for (final child in dto.subAgentIds) {
        if (child.isNotEmpty && !visited.contains(child)) queue.add(child);
      }
    }

    return (modelIds: modelIds, toolIds: toolIds, datasetIds: datasetIds);
  }

  /// 重新写入子 agent 的 JSON 文件，使用成功导出的 datasetIds
  static Future<void> _rewriteSubAgentsJson(AgentDTO root, String multiagentDirPath, Set<String> successfulDatasetIds,
      {Map<String, ToolModel>? toolMap}) async {
    final multiagentDir = Directory(multiagentDirPath);
    if (!await multiagentDir.exists()) return;

    final visited = <String>{};
    final queue = <String>[];
    queue.addAll(root.subAgentIds);

    while (queue.isNotEmpty) {
      final id = queue.removeAt(0);
      if (id.isEmpty || visited.contains(id)) continue;
      final sub = await agentRepository.getAgentFromBox(id);
      if (sub == null) continue;
      visited.add(id);

      final dto = sub.toDTO();
      final data = await _buildExportData(dto, successfulDatasetIds: successfulDatasetIds, toolMap: toolMap);
      final path = '$multiagentDirPath/${sub.id}.json';
      await File(path).writeAsString(const JsonEncoder.withIndent('  ').convert(data), encoding: utf8);

      for (final child in dto.subAgentIds) {
        if (child.isNotEmpty && !visited.contains(child)) queue.add(child);
      }
    }
  }

  static ({Set<String> modelIds, Set<String> toolIds, Set<String> datasetIds}) _collectBaseDependencies(AgentDTO agentDTO) {
    final modelIds = <String>{};
    if (agentDTO.llmModelId.isNotEmpty) modelIds.add(agentDTO.llmModelId);
    if (agentDTO.ttsModelId.isNotEmpty) modelIds.add(agentDTO.ttsModelId);
    if (agentDTO.asrModelId.isNotEmpty) modelIds.add(agentDTO.asrModelId);

    final toolIds = <String>{};
    if (agentDTO.toolFunctionList != null) {
      for (var f in agentDTO.toolFunctionList!) {
        if (f.toolId.isNotEmpty) toolIds.add(f.toolId);
      }
    }

    final datasetIds = <String>{};
    if (agentDTO.datasetIds?.isNotEmpty == true) {
      datasetIds.addAll(agentDTO.datasetIds!);
    }

    return (modelIds: modelIds, toolIds: toolIds, datasetIds: datasetIds);
  }

  static Future<void> _exportModels(Set<String> modelIds, String modelsDirPath, {bool exportPlaintext = false}) async {
    if (modelIds.isEmpty) return;
    await Directory(modelsDirPath).create();
    for (final id in modelIds) {
      final data = await modelRepository.getModelFromBox(id);
      if (data == null) continue;
      final dto = ModelConverter.modelToDto(data);
      final export = ModelExportUtil.buildModelExportData(dto, exportPlaintext: exportPlaintext);
      final path = '$modelsDirPath/${data.id}.json';
      await File(path).writeAsString(const JsonEncoder.withIndent('  ').convert(export), encoding: utf8);
    }
  }

  static Future<void> _exportTools(Set<String> toolIds, String toolsDirPath,
      {bool exportPlaintext = false, Map<String, ToolModel>? toolMap}) async {
    if (toolIds.isEmpty) return;
    await Directory(toolsDirPath).create();
    for (final id in toolIds) {
      // 优先使用传入的 toolMap，避免重复读取 Hive
      final data = toolMap?[id] ?? await toolRepository.getToolFromBox(id);
      if (data == null) continue;
      final export = await ToolExportUtil.buildToolExportData(data.toDTO(), exportPlaintext: exportPlaintext);
      final path = '$toolsDirPath/${data.id}.json';
      await File(path).writeAsString(const JsonEncoder.withIndent('  ').convert(export), encoding: utf8);
    }
  }

  static Future<({Set<String> successfulIds, bool hasError})> _exportKnowledgeBases(
      Set<String> datasetIds, String knowledgeBasesDirPath, String modelsDirPath,
      {bool exportPlaintext = false}) async {
    final successfulDatasetIds = <String>{};
    if (datasetIds.isEmpty) return (successfulIds: successfulDatasetIds, hasError: false);

    // 调用API导出知识库
    try {
      final datasetIdList = datasetIds.toList();

      // 创建临时解压目录
      final tempDir = await getTemporaryDirectory();
      final tempExtractPath = '${tempDir.path}/kb_extract_${DateTime.now().microsecondsSinceEpoch}';
      final tempExtractDir = Directory(tempExtractPath);
      await tempExtractDir.create(recursive: true);

      final tempZipPath = '$tempExtractPath/temp_datasets.zip';

      // 调用导出API下载知识库zip文件
      final success = await libraryRepository.exportKnowledge(datasetIds: datasetIdList, savePath: tempZipPath, plainText: exportPlaintext);

      if (success) {
        final zipFile = File(tempZipPath);
        if (await zipFile.exists()) {
          final bytes = await zipFile.readAsBytes();
          final archive = ZipDecoder().decodeBytes(bytes);

          // 解压所有文件到临时目录
          for (final file in archive) {
            final filename = file.name;
            if (file.isFile) {
              final data = file.content as List<int>;
              final filePath = '$tempExtractPath/$filename';
              final outputFile = File(filePath);
              await outputFile.create(recursive: true);
              await outputFile.writeAsBytes(data);
            } else if (file.isDirectory) {
              final dirPath = '$tempExtractPath/$filename';
              await Directory(dirPath).create(recursive: true);
            }
          }

          // 处理解压出来的 knowledge_bases 文件夹
          final extractedKbDir = Directory('$tempExtractPath/knowledge_bases');
          if (await extractedKbDir.exists()) {
            await Directory(knowledgeBasesDirPath).create(recursive: true);
            await _copyDirectoryWithCount(extractedKbDir, Directory(knowledgeBasesDirPath));

            // 记录成功导出的 datasetId（knowledge_bases 下的子文件夹名就是 datasetId）
            await for (final entity in extractedKbDir.list(recursive: false)) {
              if (entity is Directory) {
                final datasetId = entity.path.replaceAll('\\', '/').split('/').last;
                if (datasetIds.contains(datasetId)) {
                  successfulDatasetIds.add(datasetId);
                }
              }
            }
          }

          // 处理解压出来的 models 文件夹（embedding模型）
          final extractedModelsDir = Directory('$tempExtractPath/models');
          if (await extractedModelsDir.exists()) {
            await Directory(modelsDirPath).create(recursive: true);
            await _copyDirectoryWithCount(extractedModelsDir, Directory(modelsDirPath));
          }

          // 清理临时目录
          await tempExtractDir.delete(recursive: true);
        } else {
          // zip 未生成，视为失败
          await tempExtractDir.delete(recursive: true);
          throw Exception('Knowledge base zip file not found after download');
        }
      } else {
        Log.e("导出知识库失败", "API返回失败");
        return (successfulIds: successfulDatasetIds, hasError: true);
      }
    } catch (e) {
      Log.e("导出知识库失败", e);
      return (successfulIds: successfulDatasetIds, hasError: true);
    }

    return (successfulIds: successfulDatasetIds, hasError: false);
  }

  /// 复制目录内容，并返回复制的文件/目录数量
  static Future<({int files, int dirs})> _copyDirectoryWithCount(Directory source, Directory destination) async {
    var files = 0;
    var dirs = 0;
    await for (final entity in source.list(recursive: false)) {
      if (entity is File) {
        final newPath = '${destination.path}/${entity.path.replaceAll('\\', '/').split('/').last}';
        await entity.copy(newPath);
        files++;
      } else if (entity is Directory) {
        final newDir = Directory('${destination.path}/${entity.path.replaceAll('\\', '/').split('/').last}');
        await newDir.create(recursive: true);
        final child = await _copyDirectoryWithCount(entity, newDir);
        files += child.files;
        dirs += (child.dirs + 1);
      }
    }
    return (files: files, dirs: dirs);
  }

  static Future<void> _addDirectoryToArchiveIfExists(Archive archive, String baseTempPath, String dirPath) async {
    final dir = Directory(dirPath);
    if (!dir.existsSync()) return;

    Future<void> addRecursively(Directory d) async {
      await for (final entity in d.list(recursive: false, followLinks: false)) {
        if (entity is File) {
          final bytes = await entity.readAsBytes();
          final relative = entity.path.substring(baseTempPath.length + 1).replaceAll('\\', '/');
          archive.addFile(ArchiveFile(relative, bytes.length, bytes));
        } else if (entity is Directory) {
          await addRecursively(entity);
        }
      }
    }

    await addRecursively(dir);
  }

  // 预览目录日志已移除

  static Future<void> _addFileToArchive(Archive archive, File file, String baseTempPath) async {
    final bytes = await file.readAsBytes();
    final relative = file.path.substring(baseTempPath.length + 1).replaceAll('\\', '/');
    archive.addFile(ArchiveFile(relative, bytes.length, bytes));
  }

  static ({String modelsDirPath, String toolsDirPath, String knowledgeBasesDirPath, String multiagentDirPath}) _buildExportPaths(
      String tempPath) {
    final models = '$tempPath/models';
    final tools = '$tempPath/tools';
    final knowledgeBases = '$tempPath/knowledge_bases';
    final multi = '$tempPath/multiagent';
    return (modelsDirPath: models, toolsDirPath: tools, knowledgeBasesDirPath: knowledgeBases, multiagentDirPath: multi);
  }

  static String _generateFileName(AgentDTO agentDTO) {
    final name = agentDTO.name.isEmpty ? "Agent_${agentDTO.id.lastSixChars}" : agentDTO.name;
    return "$name.json";
  }

  static Future<Map<String, dynamic>> _buildMetadataJson() async {
    // 获取用户信息
    String author = "";
    try {
      final account = await accountRepository.getAccountInfoFromBox();
      if (account != null && account.name.isNotEmpty) {
        author = account.name;
      }
    } catch (e) {
      Log.w("获取用户信息失败: $e");
    }

    // 生成当前时间
    final now = DateTime.now();
    final createTime = "${now.year.toString().padLeft(4, '0')}-"
        "${now.month.toString().padLeft(2, '0')}-"
        "${now.day.toString().padLeft(2, '0')} "
        "${now.hour.toString().padLeft(2, '0')}:"
        "${now.minute.toString().padLeft(2, '0')}:"
        "${now.second.toString().padLeft(2, '0')}";

    return {"agent": "LiteAgent", "version": "1.0.0", "author": author, "createTime": createTime};
  }

  /// 写入metadata.json文件
  static Future<File> _writeMetadataJson(String tempPath) async {
    final metadataData = await _buildMetadataJson();
    final metadataJsonPath = '$tempPath/metadata.json';
    final metadataJsonFile = File(metadataJsonPath);
    await metadataJsonFile.writeAsString(const JsonEncoder.withIndent('  ').convert(metadataData), encoding: utf8);
    return metadataJsonFile;
  }
}
