import 'package:json_annotation/json_annotation.dart';

part 'document.g.dart';


@JsonSerializable()
class DocumentDto extends Object {

  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'userId')
  String userId;

  @JsonKey(name: 'workspaceId')
  String workspaceId;

  @JsonKey(name: 'datasetId')
  String? datasetId;

  @JsonKey(name: 'dataSourceType')
  String? dataSourceType;

  @JsonKey(name: 'filePath')
  String? filePath;

  @JsonKey(name: 'content')
  String? content;

  @JsonKey(name: 'htmlUrl')
  List<String>? htmlUrl;

  @JsonKey(name: 'md5Hash')
  String? md5Hash;

  @JsonKey(name: 'wordCount')
  int? wordCount;

  @JsonKey(name: 'tokenCount')
  int? tokenCount;

  @JsonKey(name: 'metadata')
  String? metadata;

  @JsonKey(name: 'enableFlag')
  bool? enableFlag;

  @JsonKey(name: 'createTime')
  String? createTime;

  @JsonKey(name: 'updateTime')
  String? updateTime;


  DocumentDto(this.id,this.name,this.userId,this.workspaceId,this.datasetId,this.dataSourceType,this.filePath,this.content,this.htmlUrl,this.md5Hash,this.wordCount,this.tokenCount,this.metadata,this.enableFlag,this.createTime,this.updateTime,);

  factory DocumentDto.fromJson(Map<String, dynamic> srcJson) => _$DocumentDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$DocumentDtoToJson(this);

}
