import 'package:json_annotation/json_annotation.dart';

part 'retrieval_result.g.dart';

@JsonSerializable()
class RetrievalResultDto extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'content', defaultValue: '')
  String content;

  @JsonKey(name: 'tokenCount', defaultValue: 0)
  int tokenCount;

  @JsonKey(name: 'datasetId', defaultValue: '')
  String datasetId;

  @JsonKey(name: 'documentId', defaultValue: '')
  String documentId;

  @JsonKey(name: 'documentName', defaultValue: '')
  String documentName;

  @JsonKey(name: 'score', defaultValue: 0.0)
  double score;

  @JsonKey(name: 'fileId', defaultValue: '')
  String fileId;

  RetrievalResultDto(this.id, this.content, this.tokenCount, this.datasetId, this.documentId, this.documentName, this.score, this.fileId);

  factory RetrievalResultDto.fromJson(Map<String, dynamic> srcJson) => _$RetrievalResultDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$RetrievalResultDtoToJson(this);
}
