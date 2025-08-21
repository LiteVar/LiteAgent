import 'package:json_annotation/json_annotation.dart';

part 'mcp_tool_info.g.dart';

@JsonSerializable()
class McpToolInfo {

  final String name;
  
  final String description;
  
  const McpToolInfo({
    required this.name,
    required this.description,
  });
  
  factory McpToolInfo.fromJson(Map<String, dynamic> json) => _$McpToolInfoFromJson(json);
  
  Map<String, dynamic> toJson() => _$McpToolInfoToJson(this);
} 