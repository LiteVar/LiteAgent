import 'package:json_annotation/json_annotation.dart';

part 'library.g.dart';

@JsonSerializable()
class LibraryDto extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'name', defaultValue: '')
  String name;

  @JsonKey(name: 'icon', defaultValue: '')
  String icon;

  @JsonKey(name: 'description', defaultValue: '')
  String description;

  @JsonKey(name: 'shareFlag', defaultValue: false)
  bool shareFlag;

  @JsonKey(name: 'dataSourceType', defaultValue: '')
  String dataSourceType;

  @JsonKey(name: 'docCount', defaultValue: 0)
  int docCount;

  @JsonKey(name: 'wordCount', defaultValue: 0)
  int wordCount;

  @JsonKey(name: 'agentCount', defaultValue: 0)
  int agentCount;

  ///embedding模型id
  String? embeddingModelId;

  ///LLM模型id
  String? llmModelId;

  ///导入时发现的重名模型id
  String? similarId;

  ///导入时对重名模型的操作
  int? operate;

  LibraryDto(this.id, this.name, this.icon, this.description, this.shareFlag, this.dataSourceType, this.docCount, this.wordCount,
      this.agentCount, this.embeddingModelId, this.llmModelId, this.similarId, this.operate);

  factory LibraryDto.fromJson(Map<String, dynamic> srcJson) => _$LibraryDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$LibraryDtoToJson(this);
}
