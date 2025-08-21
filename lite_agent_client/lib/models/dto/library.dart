import 'package:json_annotation/json_annotation.dart';

part 'library.g.dart';


@JsonSerializable()
class LibraryDto extends Object {

  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'userId')
  String? userId;

  @JsonKey(name: 'workspaceId')
  String? workspaceId;

  @JsonKey(name: 'icon')
  String? icon;

  @JsonKey(name: 'description')
  String? description;

  @JsonKey(name: 'shareFlag')
  bool? shareFlag;

  @JsonKey(name: 'dataSourceType')
  String? dataSourceType;

  @JsonKey(name: 'llmModelId')
  String? llmModelId;

  @JsonKey(name: 'embeddingModel')
  String? embeddingModel;

  @JsonKey(name: 'embeddingModelProvider')
  String? embeddingModelProvider;

  @JsonKey(name: 'retrievalTopK')
  int? retrievalTopK;

  @JsonKey(name: 'retrievalScoreThreshold')
  int? retrievalScoreThreshold;

  @JsonKey(name: 'createTime')
  String? createTime;

  @JsonKey(name: 'updateTime')
  String? updateTime;

  @JsonKey(name: 'docCount')
  int? docCount;

  @JsonKey(name: 'wordCount')
  int? wordCount;

  @JsonKey(name: 'agentCount')
  int? agentCount;

  LibraryDto(this.id,this.name,this.userId,this.workspaceId,this.icon,this.description,this.shareFlag,this.dataSourceType,this.llmModelId,this.embeddingModel,this.embeddingModelProvider,this.retrievalTopK,this.retrievalScoreThreshold,this.createTime,this.updateTime,);

  factory LibraryDto.fromJson(Map<String, dynamic> srcJson) => _$LibraryDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$LibraryDtoToJson(this);

}


