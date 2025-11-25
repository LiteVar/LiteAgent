import 'package:json_annotation/json_annotation.dart';

part 'retrieval_record.g.dart';


@JsonSerializable()
class RetrievalRecordDto extends Object {

  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'datasetId', defaultValue: '')
  String datasetId;

  @JsonKey(name: 'agentId', defaultValue: '')
  String agentId;

  @JsonKey(name: 'content', defaultValue: '')
  String content;

  @JsonKey(name: 'retrieveType', defaultValue: '')
  String retrieveType;

  @JsonKey(name: 'createTime', defaultValue: '')
  String createTime;

  RetrievalRecordDto(this.id,this.datasetId,this.agentId,this.content,this.retrieveType,this.createTime,);

  factory RetrievalRecordDto.fromJson(Map<String, dynamic> srcJson) => _$RetrievalRecordDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$RetrievalRecordDtoToJson(this);

}


