// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'mcp_server_status.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

McpServerStatus _$McpServerStatusFromJson(Map<String, dynamic> json) =>
    McpServerStatus(
      serverId: json['serverId'] as String,
      serverName: json['serverName'] as String,
      isAvailable: json['isAvailable'] as bool,
      tools: (json['tools'] as List<dynamic>)
          .map((e) => McpToolInfo.fromJson(e as Map<String, dynamic>))
          .toList(),
      errorMessage: json['errorMessage'] as String?,
      version: json['version'] as String?,
    );

Map<String, dynamic> _$McpServerStatusToJson(McpServerStatus instance) =>
    <String, dynamic>{
      'serverId': instance.serverId,
      'serverName': instance.serverName,
      'isAvailable': instance.isAvailable,
      'tools': instance.tools.map((e) => e.toJson()).toList(),
      'errorMessage': instance.errorMessage,
      'version': instance.version,
    };
