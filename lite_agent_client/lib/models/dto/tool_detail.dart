import 'package:json_annotation/json_annotation.dart';

part 'tool_detail.g.dart';


@JsonSerializable()
class ToolDetailDTO extends Object {

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

  @JsonKey(name: 'apiKeyType')
  String? apiKeyType;

  @JsonKey(name: 'apiKey')
  String? apiKey;

  @JsonKey(name: 'shareFlag')
  bool? shareFlag;

  @JsonKey(name: 'createTime')
  String createTime;

  @JsonKey(name: 'updateTime')
  String updateTime;

  ToolDetailDTO(this.id,this.userId,this.workspaceId,this.name,this.description,this.schemaType,this.schemaStr,this.apiKeyType,this.shareFlag,this.createTime,this.updateTime,);

  factory ToolDetailDTO.fromJson(Map<String, dynamic> srcJson) => _$ToolDetailDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ToolDetailDTOToJson(this);

}
