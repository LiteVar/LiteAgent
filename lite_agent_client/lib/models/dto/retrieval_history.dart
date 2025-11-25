import 'package:json_annotation/json_annotation.dart';

part 'retrieval_history.g.dart';

@JsonSerializable()
class RetrievalHistoryDto extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'datasetId', defaultValue: '')
  String datasetId;

  @JsonKey(name: 'datasetName', defaultValue: '')
  String datasetName;

  RetrievalHistoryDto(this.id, this.datasetId, this.datasetName);

  factory RetrievalHistoryDto.fromJson(Map<String, dynamic> srcJson) => _$RetrievalHistoryDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$RetrievalHistoryDtoToJson(this);
}
