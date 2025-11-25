import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';

/// 工具转换器 - 负责 ToolModel 和 ToolDTO 之间的转换，以及 schema 类型管理
class ToolConverter {
  /// 将 ToolDTO 转换为 ToolModel
  static ToolModel dtoToModel(ToolDTO dto) {
    final schemaType = getModelProtocol(dto.schemaType);

    return ToolModel(
      id: dto.id,
      name: dto.name,
      description: dto.description,
      schemaType: schemaType,
      schemaText: dto.schemaStr,
      apiType: dto.apiKeyType,
      apiText: dto.apiKey,
      createTime: 0,
      supportMultiAgent: dto.autoAgent,
      shareFlag: dto.shareFlag,
    );
  }

  /// 将 ToolModel 转换为 ToolDTO
  static ToolDTO modelToDto(ToolModel model) {
    final schemaType = getDtoProtocol(model.schemaType);
    final apiKeyType = normalizeApiKeyType(model.apiType);

    return ToolDTO(model.id, model.name, model.description, schemaType, model.schemaText, model.apiText, apiKeyType, model.shareFlag,
        model.supportMultiAgent ?? false, null, "", 0);
  }

  /// 将 DTO 的协议类型标识（int）转换为 Model 的协议类型标识（String）
  /// 映射关系：1→Protocol.OPENAPI, 2→Protocol.JSONRPCHTTP, 4→Protocol.OPENTOOL, 5→Protocol.MCP_STDIO, 6→"OpenTool"
  static String getModelProtocol(int dtoProtocol) {
    return switch (dtoProtocol) {
      ToolValidator.DTO_OPENAPI => Protocol.OPENAPI,
      ToolValidator.DTO_JSON_RPC_HTTP => Protocol.JSONRPCHTTP,
      ToolValidator.DTO_OPEN_TOOL => Protocol.OPENTOOL,
      ToolValidator.DTO_MCP => Protocol.MCP_STDIO,
      ToolValidator.DTO_OPEN_TOOL_SERVER => ToolValidator.OPTION_OPENTOOL_SERVER,
      _ => "",
    };
  }

  /// 将 Model 的协议类型标识（String）转换为 DTO 的协议类型标识（int）
  /// 映射关系：Protocol.OPENAPI→1, Protocol.JSONRPCHTTP→2, Protocol.OPENTOOL→4, Protocol.MCP_STDIO→5, "OpenTool"→6
  static int getDtoProtocol(String modelProtocol) {
    return switch (modelProtocol) {
      Protocol.OPENAPI => ToolValidator.DTO_OPENAPI,
      Protocol.JSONRPCHTTP => ToolValidator.DTO_JSON_RPC_HTTP,
      Protocol.OPENTOOL => ToolValidator.DTO_OPEN_TOOL,
      Protocol.MCP_STDIO => ToolValidator.DTO_MCP,
      ToolValidator.OPTION_OPENTOOL_SERVER => ToolValidator.DTO_OPEN_TOOL_SERVER,
      _ => 0,
    };
  }

  /// 标准化 API Key 类型
  static String normalizeApiKeyType(String apiType) {
    return switch (apiType.toLowerCase()) {
      "basic" => "Basic",
      "bearer" => "Bearer",
      _ => apiType,
    };
  }

  static String getSchemaTypeOptionString(String type) {
    return switch (type) {
      Protocol.OPENAPI => ToolValidator.OPTION_OPENAPI,
      Protocol.JSONRPCHTTP => ToolValidator.OPTION_JSONRPCHTTP,
      Protocol.OPENTOOL => ToolValidator.OPTION_OPENTOOL,
      Protocol.MCP_STDIO => ToolValidator.OPTION_MCP_STDIO_TOOLS,
      ToolValidator.OPTION_OPENTOOL_SERVER => ToolValidator.OPTION_OPENTOOL_SERVER,
      _ => "",
    };
  }
}
