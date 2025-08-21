import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/retrieval_record.dart';

part 'retrieval_record_page.g.dart';


@JsonSerializable()
class RetrievalRecordPageDto extends Object {

  @JsonKey(name: 'pageNo')
  int pageNo;

  @JsonKey(name: 'pageSize')
  int pageSize;

  @JsonKey(name: 'total')
  String total;

  @JsonKey(name: 'list')
  List<RetrievalRecordDto> list;

  RetrievalRecordPageDto(this.pageNo,this.pageSize,this.total,this.list,);

  factory RetrievalRecordPageDto.fromJson(Map<String, dynamic> srcJson) => _$RetrievalRecordPageDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$RetrievalRecordPageDtoToJson(this);

}