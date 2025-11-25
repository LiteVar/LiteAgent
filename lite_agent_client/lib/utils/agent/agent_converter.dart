import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/dto/function.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/utils/agent/agent_validator.dart';

/// Agent转换工具类
/// 负责 AgentModel 和 AgentDTO 之间的转换
class AgentConverter {
  static String typeToString(int? type) {
    switch (type) {
      case AgentValidator.DTO_TYPE_DISTRIBUTE:
        return AgentValidator.TYPE_DISTRIBUTE;
      case AgentValidator.DTO_TYPE_REFLECTION:
        return AgentValidator.TYPE_REFLECTION;
      case AgentValidator.DTO_TYPE_GENERAL:
      default:
        return AgentValidator.TYPE_GENERAL;
    }
  }

  static String modeToString(int? mode) {
    switch (mode) {
      case AgentValidator.DTO_MODE_SERIAL:
        return AgentValidator.MODE_SERIAL;
      case AgentValidator.DTO_MODE_REJECT:
        return AgentValidator.MODE_REJECT;
      case AgentValidator.DTO_MODE_PARALLEL:
      default:
        return AgentValidator.MODE_PARALLEL;
    }
  }

  static int stringToType(String? value) {
    if (value == null) return AgentValidator.DTO_TYPE_GENERAL;
    final s = value.trim();
    if (s.isEmpty) return AgentValidator.DTO_TYPE_GENERAL;
    final n = int.tryParse(s);
    if (n != null) return n;
    switch (s.toUpperCase()) {
      case AgentValidator.TYPE_DISTRIBUTE:
        return AgentValidator.DTO_TYPE_DISTRIBUTE;
      case AgentValidator.TYPE_REFLECTION:
        return AgentValidator.DTO_TYPE_REFLECTION;
      case AgentValidator.TYPE_GENERAL:
      default:
        return AgentValidator.DTO_TYPE_GENERAL;
    }
  }

  static int stringToMode(String? value) {
    if (value == null) return AgentValidator.DTO_MODE_PARALLEL;
    final s = value.trim();
    if (s.isEmpty) return AgentValidator.DTO_MODE_PARALLEL;
    final n = int.tryParse(s);
    if (n != null) return n;
    switch (s.toUpperCase()) {
      case AgentValidator.MODE_SERIAL:
        return AgentValidator.DTO_MODE_SERIAL;
      case AgentValidator.MODE_REJECT:
        return AgentValidator.DTO_MODE_REJECT;
      case AgentValidator.MODE_PARALLEL:
      default:
        return AgentValidator.DTO_MODE_PARALLEL;
    }
  }

  /// DTO -> Model 转换
  static AgentModel dtoToModel(AgentDTO dto) {
    var functionList = <ToolFunctionModel>[];
    if (dto.toolFunctionList != null) {
      for (var functionDto in dto.toolFunctionList!) {
        var functionModel = ToolFunctionModel();
        functionModel.translateFromDTO(functionDto);
        functionList.add(functionModel);
      }
    }

    return AgentModel(
      id: dto.id,
      name: dto.name,
      iconPath: dto.icon,
      description: dto.description,
      modelId: dto.llmModelId,
      prompt: dto.prompt,
      temperature: dto.temperature,
      maxToken: dto.maxTokens,
      topP: dto.topP,
      isCloud: dto.isCloud,
      functionList: functionList,
      libraryIds: dto.datasetIds,
      operationMode: dto.mode,
      agentType: dto.type,
      childAgentIds: dto.subAgentIds,
      ttsModelId: dto.ttsModelId,
      asrModelId: dto.asrModelId,
      autoAgentFlag: dto.autoAgentFlag,
      shareFlag: dto.shareTip,
    );
  }

  /// Model -> DTO 转换
  static AgentDTO modelToDto(AgentModel model) {
    var webFunctionList = <FunctionDto>[];
    if (model.functionList != null) {
      for (var function in model.functionList!) {
        var dto = function.translateToDTO();
        dto.mode = model.toolOperationMode ?? 0;
        webFunctionList.add(dto);
      }
    }

    return AgentDTO(
      model.id,
      model.name,
      model.iconPath,
      model.description,
      model.prompt,
      model.modelId,
      model.shareFlag,
      model.temperature,
      model.topP,
      model.maxToken,
      webFunctionList,
      model.childAgentIds ?? [],
      model.agentType ?? 0,
      model.operationMode ?? 0,
      model.libraryIds,
      model.isCloud,
      model.autoAgentFlag ?? false,
      model.ttsModelId ?? "",
      model.asrModelId ?? "",
      "",
      0,
    );
  }
}
