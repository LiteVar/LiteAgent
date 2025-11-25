import 'package:json_annotation/json_annotation.dart';

part 'segment.g.dart';

@JsonSerializable()
class SegmentDto extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'datasetId', defaultValue: '')
  String datasetId;

  @JsonKey(name: 'documentId', defaultValue: '')
  String documentId;

  @JsonKey(name: 'content', defaultValue: '')
  String content;

  @JsonKey(name: 'wordCount', defaultValue: 0)
  int wordCount;

  @JsonKey(name: 'tokenCount', defaultValue: 0)
  int tokenCount;

  @JsonKey(name: 'hitCount', defaultValue: 0)
  int hitCount;

  @JsonKey(name: 'enableFlag', defaultValue: true)
  bool enableFlag;

  @JsonKey(name: 'createTime', defaultValue: '')
  String createTime;

  @JsonKey(name: 'score')
  double? score;

  SegmentDto(this.id, this.datasetId, this.documentId, this.content, this.wordCount, this.tokenCount, this.hitCount, this.enableFlag,
      this.createTime, this.score);

  factory SegmentDto.fromJson(Map<String, dynamic> srcJson) => _$SegmentDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$SegmentDtoToJson(this);
}
