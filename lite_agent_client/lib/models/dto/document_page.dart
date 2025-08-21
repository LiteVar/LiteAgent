import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/document.dart';

part 'document_page.g.dart';


@JsonSerializable()
class DocumentPageDto extends Object {

  @JsonKey(name: 'pageNo')
  int pageNo;

  @JsonKey(name: 'pageSize')
  int pageSize;

  @JsonKey(name: 'total')
  String total;

  @JsonKey(name: 'list')
  List<DocumentDto> list;

  DocumentPageDto(this.pageNo,this.pageSize,this.total,this.list,);

  factory DocumentPageDto.fromJson(Map<String, dynamic> srcJson) => _$DocumentPageDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$DocumentPageDtoToJson(this);

}
