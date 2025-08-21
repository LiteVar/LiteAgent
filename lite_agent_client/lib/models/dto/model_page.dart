import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/model.dart';

part 'model_page.g.dart';


@JsonSerializable()
class ModelPageDto extends Object {

  @JsonKey(name: 'pageNo')
  int pageNo;

  @JsonKey(name: 'pageSize')
  int pageSize;

  @JsonKey(name: 'total')
  String total;

  @JsonKey(name: 'list')
  List<ModelDTO> list;

  ModelPageDto(this.pageNo,this.pageSize,this.total,this.list,);

  factory ModelPageDto.fromJson(Map<String, dynamic> srcJson) => _$ModelPageDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ModelPageDtoToJson(this);

}