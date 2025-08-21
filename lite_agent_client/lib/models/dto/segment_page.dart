import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/segment.dart';

part 'segment_page.g.dart';


@JsonSerializable()
class SegmentPageDto extends Object {

  @JsonKey(name: 'pageNo')
  int pageNo;

  @JsonKey(name: 'pageSize')
  int pageSize;

  @JsonKey(name: 'total')
  String total;

  @JsonKey(name: 'list')
  List<SegmentDto> list;

  SegmentPageDto(this.pageNo,this.pageSize,this.total,this.list,);

  factory SegmentPageDto.fromJson(Map<String, dynamic> srcJson) => _$SegmentPageDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$SegmentPageDtoToJson(this);

}