import 'package:json_annotation/json_annotation.dart';

part 'retrieval_record.g.dart';


@JsonSerializable()
class RetrievalRecordDto extends Object {

  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'datasetId')
  String datasetId;

  @JsonKey(name: 'agentId')
  String agentId;

  @JsonKey(name: 'content')
  String content;

  @JsonKey(name: 'retrieveType')
  String retrieveType;

  @JsonKey(name: 'createTime')
  String createTime;

  RetrievalRecordDto(this.id,this.datasetId,this.agentId,this.content,this.retrieveType,this.createTime,);

  factory RetrievalRecordDto.fromJson(Map<String, dynamic> srcJson) => _$RetrievalRecordDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$RetrievalRecordDtoToJson(this);

}


