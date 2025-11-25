import 'package:json_annotation/json_annotation.dart';

import 'function.dart';

part 'tool.g.dart';

@JsonSerializable()
class ToolDTO extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'name', defaultValue: '')
  String name;

  @JsonKey(name: 'description', defaultValue: '')
  String description;

  @JsonKey(name: 'schemaType', defaultValue: 0)
  int schemaType;

  @JsonKey(name: 'schemaStr', defaultValue: '')
  String schemaStr;

  @JsonKey(name: 'apiKey', defaultValue: '')
  String apiKey;

  @JsonKey(name: 'apiKeyType', defaultValue: '')
  String apiKeyType;

  @JsonKey(name: 'shareFlag', defaultValue: false)
  bool shareFlag;

  @JsonKey(name: 'autoAgent', defaultValue: false)
  bool autoAgent;

  @JsonKey(name: 'functionList')
  List<FunctionDto>? functionList;

  ///导入时发现的重名模型id
  String? similarId;

  ///导入时对重名模型的操作
  int? operate;

  ToolDTO(this.id, this.name, this.description, this.schemaType, this.schemaStr, this.apiKey, this.apiKeyType, this.shareFlag,
      this.autoAgent, this.functionList, this.similarId, this.operate);

  factory ToolDTO.fromJson(Map<String, dynamic> srcJson) => _$ToolDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ToolDTOToJson(this);
}
