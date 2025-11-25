import 'package:json_annotation/json_annotation.dart';

part 'document.g.dart';

@JsonSerializable()
class DocumentDto extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'name', defaultValue: '')
  String name;

  @JsonKey(name: 'datasetId', defaultValue: '')
  String datasetId;

  @JsonKey(name: 'dataSourceType', defaultValue: '')
  String dataSourceType;

  @JsonKey(name: 'content', defaultValue: '')
  String content;

  @JsonKey(name: 'htmlUrl')
  List<String>? htmlUrl;

  @JsonKey(name: 'wordCount', defaultValue: 0)
  int wordCount;

  @JsonKey(name: 'tokenCount', defaultValue: 0)
  int tokenCount;

  @JsonKey(name: 'enableFlag', defaultValue: true)
  bool enableFlag;

  @JsonKey(name: 'fileId', defaultValue: '')
  String fileId;

  DocumentDto(this.id, this.name, this.datasetId, this.dataSourceType, this.content, this.htmlUrl, this.wordCount, this.tokenCount,
      this.enableFlag, this.fileId);

  factory DocumentDto.fromJson(Map<String, dynamic> srcJson) => _$DocumentDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$DocumentDtoToJson(this);
}
