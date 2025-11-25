import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

import '../../models/local/agent.dart';

/// Agent验证工具类
/// 负责Agent配置的验证
class AgentValidator {
  // 字符串常量
  static const String TYPE_GENERAL = "GENERAL";
  static const String TYPE_DISTRIBUTE = "DISTRIBUTE";
  static const String TYPE_REFLECTION = "REFLECTION";

  static const String MODE_PARALLEL = "PARALLEL";
  static const String MODE_SERIAL = "SERIAL";
  static const String MODE_REJECT = "REJECT";

  // 数值常量
  static const int DTO_TYPE_GENERAL = 0;
  static const int DTO_TYPE_DISTRIBUTE = 1;
  static const int DTO_TYPE_REFLECTION = 2;

  static const int DTO_MODE_PARALLEL = 0;
  static const int DTO_MODE_SERIAL = 1;
  static const int DTO_MODE_REJECT = 2;

  /// 验证Agent JSON配置
  static bool validateAgentJson(Map<String, dynamic> jsonData) {
    const requiredFields = ['id', 'name', 'type', 'mode'];

    // 检查必需字段是否存在、类型正确且非空
    for (final field in requiredFields) {
      final value = jsonData[field];
      if (value == null || value is! String || value.trim().isEmpty) {
        return false;
      }
    }

    // 检查类型字段
    return validateAgentType(jsonData['type']) && validateAgentMode(jsonData['mode']);
  }

  /// 验证Agent类型
  static bool validateAgentType(String type) {
    const validTypes = [TYPE_GENERAL, TYPE_DISTRIBUTE, TYPE_REFLECTION];
    return validTypes.contains(type);
  }

  /// 验证Agent模式
  static bool validateAgentMode(String mode) {
    const validModes = [MODE_PARALLEL, MODE_SERIAL, MODE_REJECT];
    return validModes.contains(mode);
  }

  /// 获取相同名字的工具ID
  static String? getSameNameAgentId(String name, List<AgentModel> existingAgents, {String? excludeId}) {
    if (name.trim().isEmpty) return null;

    for (final agent in existingAgents) {
      // 如果指定了排除ID，则跳过该工具
      if (excludeId != null && agent.id == excludeId) continue;
      if (agent.name.trimmed() == name.trimmed()) {
        return agent.id;
      }
    }
    return null;
  }

  /// 检查工具名字是否唯一
  static bool isNameUnique(String name, List<AgentModel> existingAgents, {String? excludeId}) {
    return getSameNameAgentId(name, existingAgents, excludeId: excludeId) == null;
  }

  /// 检查工具名字是否唯一（异步版本）
  static Future<bool> isNameUniqueAsync(String name, {String? excludeId}) async {
    if (name.trim().isEmpty) return false;
    final existingAgents = (await agentRepository.getAgentListFromBox()).toList();
    return getSameNameAgentId(name, existingAgents, excludeId: excludeId) == null;
  }
}
