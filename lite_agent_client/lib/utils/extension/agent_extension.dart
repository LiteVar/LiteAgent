import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/utils/agent/agent_converter.dart';
import 'package:lite_agent_client/utils/agent/agent_export.dart';

/// Agent扩展方法
/// 提供便捷的转换、导出和导入方法
extension AgentDTOExtensions on AgentDTO {
  /// DTO -> Model 转换（委托 AgentConverter）
  AgentModel toModel() => AgentConverter.dtoToModel(this);

  /// 导出Agent为ZIP文件（委托 AgentExportUtil）
  Future<String?> exportZip() => AgentExportUtil.exportAgentToZip(this);
}

extension AgentModelExtensions on AgentModel {
  /// Model -> DTO 转换（委托 AgentConverter）
  AgentDTO toDTO() => AgentConverter.modelToDto(this);

  /// 导出Agent为ZIP文件（委托 AgentExportUtil）
  Future<String?> exportZip(bool exportPlaintext) => AgentExportUtil.exportAgentToZip(this.toDTO(), exportPlaintext: exportPlaintext);
}
