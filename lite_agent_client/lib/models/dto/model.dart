import 'package:json_annotation/json_annotation.dart';

part 'model.g.dart';


@JsonSerializable()
class ModelDTO extends Object {

  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'baseUrl')
  String baseUrl;

  @JsonKey(name: 'apiKey')
  String apiKey;

  @JsonKey(name: 'userId')
  String userId;

  @JsonKey(name: 'workspaceId')
  String workspaceId;

  @JsonKey(name: 'shareFlag')
  bool shareFlag;

  @JsonKey(name: 'createTime')
  String createTime;

  @JsonKey(name: 'updateTime')
  String updateTime;

  ModelDTO(this.id,this.name,this.baseUrl,this.apiKey,this.userId,this.workspaceId,this.shareFlag,this.createTime,this.updateTime,);

  factory ModelDTO.fromJson(Map<String, dynamic> srcJson) => _$ModelDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ModelDTOToJson(this);

}

