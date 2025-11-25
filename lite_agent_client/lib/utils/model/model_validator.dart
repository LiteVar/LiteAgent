import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/model/model_export.dart';
import 'dart:convert';

/// 模型验证工具类
/// 负责模型配置的验证
class ModelValidator {
  static const String LLM = "LLM";
  static const String ASR = "asr";
  static const String TTS = "tts";
  static const String EMBEDDING = "embedding";
  static const String IAMGE = "image";

  /// 验证模型DTO JSON配置
  static bool validateModelDTOJsonConfig(Map<String, dynamic> jsonData) {
    const requiredFields = ['name', 'baseUrl', 'apiKey', 'alias'];

    // 检查必需字段是否存在、类型正确且非空
    for (final field in requiredFields) {
      final value = jsonData[field];
      if (value == null || value is! String || value.trim().isEmpty) {
        return false;
      }
    }

    // 检查类型字段
    return validateModelType(jsonData['type'] ?? LLM);
  }

  /// 验证模型类型
  static bool validateModelType(String type) {
    const validTypes = [LLM, ASR, TTS, EMBEDDING];
    return validTypes.contains(type);
  }

  /// 获取相同别名的模型ID
  static String? getSameAliasModelId(String alias, List<ModelData> existingModels, {String? excludeId}) {
    if (alias.trim().isEmpty) return null;

    for (final model in existingModels) {
      // 如果指定了排除ID，则跳过该模型
      if (excludeId != null && model.id == excludeId) continue;
      if (model.alias?.trimmed() == alias.trimmed()) {
        return model.id;
      }
    }
    return null;
  }

  /// 检查模型别名是否唯一
  static bool isAliasUnique(String alias, List<ModelData> existingModels, {String? excludeId}) {
    return getSameAliasModelId(alias, existingModels, excludeId: excludeId) == null;
  }

  /// 检查模型别名是否唯一（异步版本）
  static Future<bool> isAliasUniqueAsync(String alias, {String? excludeId}) async {
    if (alias.trim().isEmpty) return false;
    final existingModels = (await modelRepository.getModelListFromBox()).toList();
    return getSameAliasModelId(alias, existingModels, excludeId: excludeId) == null;
  }
}
