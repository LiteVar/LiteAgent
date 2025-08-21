import 'package:json_annotation/json_annotation.dart';

part 'retrieval_result.g.dart';

@JsonSerializable()
class RetrievalResultDto extends Object {
  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'content')
  String? content;

  @JsonKey(name: 'tokenCount')
  int? tokenCount;

  @JsonKey(name: 'documentName')
  String? documentName;

  @JsonKey(name: 'score')
  double? score;

  RetrievalResultDto(this.id, this.content, this.tokenCount, this.documentName, this.score);

  factory RetrievalResultDto.fromJson(Map<String, dynamic> srcJson) => _$RetrievalResultDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$RetrievalResultDtoToJson(this);
}
