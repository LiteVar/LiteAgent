import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';
import 'package:lite_agent_client/utils/snowflake_util.dart';

/// 模型转换工具类
/// 负责 ModelData 和 ModelDTO 之间的转换
class ModelConverter {
  /// DTO -> Model 转换
  static ModelData dtoToModel(ModelDTO dto) {
    return ModelData(
      id: dto.id,
      name: dto.name,
      url: dto.baseUrl,
      key: dto.apiKey,
      maxToken: dto.maxTokens.toString(),
      createTime: DateTime.now().microsecondsSinceEpoch,
      type: dto.type,
      alias: dto.alias,
      supportMultiAgent: dto.autoAgent,
      supportToolCalling: dto.toolInvoke,
      supportDeepThinking: dto.deepThink,
    );
  }

  /// Model -> DTO 转换
  static ModelDTO modelToDto(ModelData model) {
    int? maxTokens = int.tryParse(model.maxToken ?? "4096");
    String type = model.type ?? ModelValidator.LLM;
    if (type.isNotEmpty && type != ModelValidator.LLM) {
      type = type.toLowerCase();
    }
    bool autoAgent = model.supportMultiAgent ?? false;
    bool toolInvoke = model.supportToolCalling ?? true;
    bool deepThink = model.supportDeepThinking ?? false;

    return ModelDTO(
        model.id, model.alias ?? "", model.name, model.url, model.key, maxTokens ?? 4096, type, autoAgent, toolInvoke, deepThink, "", 0);
  }

  /// 标准化模型类型
  static String normalizeModelType(String type) {
    if (type == ModelValidator.LLM) {
      return type;
    }
    return type.toLowerCase();
  }
}
