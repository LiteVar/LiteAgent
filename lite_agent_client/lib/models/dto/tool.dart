import 'package:json_annotation/json_annotation.dart';

part 'tool.g.dart';

List<ToolDTO> getToolDTOList(List<dynamic> list) {
  List<ToolDTO> result = [];
  for (var item in list) {
    result.add(ToolDTO.fromJson(item));
  }
  return result;
}

@JsonSerializable()
class ToolDTO extends Object {
  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'userId')
  String? userId;

  @JsonKey(name: 'workspaceId')
  String? workspaceId;

  @JsonKey(name: 'name')
  String? name;

  @JsonKey(name: 'description')
  String? description;

  @JsonKey(name: 'schemaType')
  int? schemaType;

  @JsonKey(name: 'schemaStr')
  String? schemaStr;

  @JsonKey(name: 'openSchemaStr')
  String? openSchemaStr;

  @JsonKey(name: 'apiKey')
  String? apiKey;

  @JsonKey(name: 'apiKeyType')
  String? apiKeyType;

  @JsonKey(name: 'shareFlag')
  bool? shareFlag;

  @JsonKey(name: 'createTime')
  String? createTime;

  @JsonKey(name: 'updateTime')
  String? updateTime;

  ToolDTO(
    this.id,
    this.userId,
    this.workspaceId,
    this.name,
    this.description,
    this.schemaType,
    this.schemaStr,
    this.openSchemaStr,
    this.apiKey,
    this.apiKeyType,
    this.shareFlag,
    this.createTime,
    this.updateTime,
  );

  factory ToolDTO.fromJson(Map<String, dynamic> srcJson) => _$ToolDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ToolDTOToJson(this);
}
