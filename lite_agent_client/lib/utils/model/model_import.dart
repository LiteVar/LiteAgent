import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/utils/extension/model_extension.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';
import 'package:lite_agent_client/utils/batch_id_generator.dart';
import 'package:lite_agent_client/config/constants.dart';

typedef ModelImportValidationResult = ({List<ModelDTO> validModels, List<String> errors});

class ModelImportUtil {
  /// 选择并验证模型配置文件（主要入口方法）
  static Future<ModelImportValidationResult?> selectAndValidateImportFiles() async {
    final files = await selectModelFiles();
    if (files == null) return null;
    return await validateModelFiles(files);
  }

  /// 验证单个模型文件的有效性
  /// 返回验证结果，包含模型DTO（如果有效）和错误信息
  static Future<({ModelDTO? model, String? error})> validateSingleModelFile(File file, {bool enableEmbeddingModel = false}) async {
    final fileName = file.path.split(Platform.pathSeparator).last;

    try {
      final content = await file.readAsString();

      if (!content.isJson()) {
        return (model: null, error: "文件 $fileName 不是有效的JSON格式");
      }

      final modelDTO = _parseModelFromJson(content, fileName);
      if (modelDTO == null) {
        return (model: null, error: "文件 $fileName 格式不正确");
      }

      // 检查是否是 embedding 模型
      if (!enableEmbeddingModel && modelDTO.type.toLowerCase() == 'embedding') {
        return (model: null, error: "文件 $fileName 中的模型类型为 embedding，暂不支持导入");
      }

      return (model: modelDTO, error: null);
    } catch (e) {
      return (model: null, error: "读取文件 $fileName 失败: $e");
    }
  }

  /// 验证模型JSON文件的有效性（组合验证方法）
  static Future<ModelImportValidationResult> validateModelFiles(List<PlatformFile> files) async {
    final validModels = <ModelDTO>[];
    final allErrors = <String>[];
    final modelToFileName = <ModelDTO, String>{};

    // 逐个验证文件
    for (final platformFile in files) {
      final filePath = platformFile.path;
      if (filePath == null) {
        allErrors.add("文件 ${platformFile.name} 路径为空");
        continue;
      }

      final file = File(filePath);
      final fileName = filePath.split(Platform.pathSeparator).last;
      final result = await validateSingleModelFile(file);
      final error = result.error;
      final model = result.model;
      if (error != null) {
        allErrors.add(error);
      } else if (model != null) {
        validModels.add(model);
        modelToFileName[model] = fileName;
      }
    }

    // 验证别名冲突
    final aliasErrors = await validateModelAliases(validModels, modelToFileName);
    allErrors.addAll(aliasErrors);

    // 过滤掉别名冲突的模型
    final finalValidModels = validModels.where((model) {
      final fileName = modelToFileName[model] ?? "未知文件";

      // 跳过别名冲突的模型
      for (var aliasError in aliasErrors) {
        if (aliasError.contains(fileName) && aliasError.contains(model.alias)) {
          Log.i("跳过别名冲突模型: ${model.name} (别名: ${model.alias})");
          return false;
        }
      }

      return true;
    }).toList();

    return (validModels: finalValidModels, errors: allErrors);
  }

  /// 批量导入模型到数据库
  static Future<int> importModels(List<ModelDTO> modelsToImport) async {
    final modelsToSave = <String, ModelData>{};

    // 批量生成唯一ID，避免重复
    final batchIds = BatchIdGenerator.instance.generateBatchIds(modelsToImport.length);

    // 转换所有模型为ModelData并分配ID
    for (int i = 0; i < modelsToImport.length; i++) {
      final modelData = _createImportModelData(modelsToImport[i], batchIds[i]);
      modelsToSave[modelData.id] = modelData;
    }

    if (modelsToSave.isNotEmpty) {
      await ModelRepository().updateModels(modelsToSave);
      Log.i("批量导入模型成功: ${modelsToSave.length} 个");
    }

    return modelsToSave.length;
  }

  /// 为Agent导入单个模型，返回带该ID的ModelDTO
  static Future<ModelDTO> importSingleModelForAgent(ModelDTO modelToImport, String newId, int operate) async {
    // 跳过操作：直接使用现有模型，不创建新模型
    if (operate == ImportOperate.operateSkip) {
      final existingModel = await ModelRepository().getData(modelToImport.similarId);
      if (existingModel == null) {
        // 如果相似模型不存在，降级为覆盖操作
        Log.i("相似模型 ${modelToImport.similarId} 不存在，降级为覆盖操作");
        return importSingleModelForAgent(modelToImport, newId, ImportOperate.operateOverwrite);
      }
      return existingModel.toDTO();
    }

    final String targetId;
    final similarModel = await ModelRepository().getData(modelToImport.similarId);
    if (modelToImport.similarId.isEmpty || operate == ImportOperate.operateNew) {
      targetId = newId;
      // while (!await ModelValidator.isAliasUniqueAsync(modelToImport.alias)) {
      //   modelToImport.alias += "_1";
      // }
    } else {
      targetId = modelToImport.similarId;
    }
    final modelData = _createImportModelData(modelToImport, targetId);
    if (operate == ImportOperate.operateOverwrite && similarModel?.createTime != null) {
      modelData.createTime = similarModel?.createTime;
    }
    await ModelRepository().updateModel(modelData.id, modelData);

    Log.i("导入单个模型成功(operate: $operate): ${modelData.alias} (${modelData.id})");
    return modelData.toDTO();
  }

  /// 选择模型配置文件
  static Future<List<PlatformFile>?> selectModelFiles() async {
    try {
      final result = await FilePicker.platform
          .pickFiles(type: FileType.custom, allowedExtensions: ['json'], allowMultiple: true, dialogTitle: '选择要导入的模型配置文件');
      return result?.files;
    } catch (e) {
      Log.e("选择文件失败", e);
      return null;
    }
  }

  /// 解析模型文件，返回有效模型和文件错误
  static Future<({List<ModelDTO> validModels, List<String> fileErrors, Map<ModelDTO, String> modelToFileName})> parseModelFiles(
      List<File> files) async {
    final validModels = <ModelDTO>[];
    final fileErrors = <String>[];
    final modelToFileName = <ModelDTO, String>{};

    for (final file in files) {
      try {
        final content = await file.readAsString();
        final fileName = file.path.split(Platform.pathSeparator).last;

        if (!content.isJson()) {
          fileErrors.add("文件 $fileName 不是有效的JSON格式");
          continue;
        }

        final modelDTO = _parseModelFromJson(content, fileName);
        if (modelDTO != null) {
          validModels.add(modelDTO);
          modelToFileName[modelDTO] = fileName;
        } else {
          fileErrors.add("文件 $fileName 格式不正确");
        }
      } catch (e) {
        final safeName = file.path.split(Platform.pathSeparator).last;
        fileErrors.add("读取文件 $safeName 失败: $e");
      }
    }

    return (validModels: validModels, fileErrors: fileErrors, modelToFileName: modelToFileName);
  }

  /// 验证模型别名唯一性，返回别名错误
  static Future<List<String>> validateModelAliases(List<ModelDTO> models, [Map<ModelDTO, String>? modelToFileName]) async {
    final aliasErrors = <String>[];
    if (models.isEmpty) return aliasErrors;

    final existingModels = (await ModelRepository().getModelListFromBox()).toList();
    final importAliases = <String, String>{};
    final conflictModels = <ModelDTO>[];

    for (final model in models) {
      final alias = model.alias;
      // 如果没有提供文件名映射，使用模型名称或别名作为标识
      final identifier = modelToFileName?[model] ?? (model.name.isNotEmpty ? model.name : model.alias);

      // 检查与现有模型的别名冲突
      if (!ModelValidator.isAliasUnique(alias, existingModels)) {
        aliasErrors.add("模型 '$identifier' 中的连接别名 '$alias' 已存在于现有模型中");
        conflictModels.add(model);
      } else if (importAliases.containsKey(alias)) {
        aliasErrors.add("导入的模型中有多个模型使用了相同的连接别名 '$alias'");
        conflictModels.add(model);
      } else {
        importAliases[alias] = identifier;
      }
    }

    return aliasErrors;
  }

  /// 从JSON内容解析模型DTO
  static ModelDTO? _parseModelFromJson(String jsonContent, String fileName) {
    try {
      final jsonData = json.decode(jsonContent) as Map<String, dynamic>;

      if (!ModelValidator.validateModelDTOJsonConfig(jsonData)) {
        Log.w("文件 $fileName 中的模型配置无效");
        return null;
      }

      return ModelDTO.fromJson(jsonData);
    } catch (e) {
      Log.e("解析模型文件 $fileName 失败", e);
      return null;
    }
  }

  /// 创建导入用的ModelData
  static ModelData _createImportModelData(ModelDTO modelToImport, String newId) {
    final modelData = modelToImport.toModel();
    modelData.id = newId;
    modelData.createTime = DateTime.now().microsecondsSinceEpoch;
    return modelData;
  }
}
