import 'package:json_annotation/json_annotation.dart';
import 'mcp_tool_info.dart';

part 'mcp_server_status.g.dart';

@JsonSerializable(explicitToJson: true)
class McpServerStatus {
  /// 服务器ID（配置文件中的key）
  final String serverId;

  final String serverName;

  final bool isAvailable;
  
  final List<McpToolInfo> tools;
  
  /// 错误信息（当isAvailable为false时）
  final String? errorMessage;

  final String? version;
  
  const McpServerStatus({
    required this.serverId,
    required this.serverName,
    required this.isAvailable,
    required this.tools,
    this.errorMessage,
    this.version,
  });

  factory McpServerStatus.available({
    required String serverId,
    required String serverName,
    required List<McpToolInfo> tools,
    String? version,
  }) {
    return McpServerStatus(
      serverId: serverId,
      serverName: serverName,
      isAvailable: true,
      tools: tools,
      version: version,
    );
  }

  factory McpServerStatus.unavailable({
    required String serverId,
    required String errorMessage,
    String? serverName,
  }) {
    return McpServerStatus(
      serverId: serverId,
      serverName: serverName ?? serverId,
      isAvailable: false,
      tools: const [],
      errorMessage: errorMessage,
    );
  }
  
  factory McpServerStatus.fromJson(Map<String, dynamic> json) => _$McpServerStatusFromJson(json);
  
  Map<String, dynamic> toJson() => _$McpServerStatusToJson(this);
} 