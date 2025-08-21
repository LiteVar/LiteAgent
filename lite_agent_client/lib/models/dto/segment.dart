import 'package:json_annotation/json_annotation.dart';

part 'segment.g.dart';


@JsonSerializable()
class SegmentDto extends Object {

  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'userId')
  String userId;

  @JsonKey(name: 'workspaceId')
  String workspaceId;

  @JsonKey(name: 'datasetId')
  String datasetId;

  @JsonKey(name: 'documentId')
  String documentId;

  @JsonKey(name: 'embeddingId')
  String embeddingId;

  @JsonKey(name: 'vectorCollectionName')
  String vectorCollectionName;

  @JsonKey(name: 'content')
  String content;

  @JsonKey(name: 'metadata')
  String metadata;

  @JsonKey(name: 'wordCount')
  int wordCount;

  @JsonKey(name: 'tokenCount')
  int tokenCount;

  @JsonKey(name: 'hitCount')
  int hitCount;

  @JsonKey(name: 'enableFlag')
  bool enableFlag;

  @JsonKey(name: 'createTime')
  String createTime;

  @JsonKey(name: 'score')
  double? score;

  SegmentDto(this.id,this.userId,this.workspaceId,this.datasetId,this.documentId,this.embeddingId,this.vectorCollectionName,this.content,this.metadata,this.wordCount,this.tokenCount,this.hitCount,this.enableFlag,this.createTime,this.score,);

  factory SegmentDto.fromJson(Map<String, dynamic> srcJson) => _$SegmentDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$SegmentDtoToJson(this);

}


