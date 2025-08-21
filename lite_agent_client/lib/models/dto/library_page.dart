import 'package:json_annotation/json_annotation.dart';
import 'package:lite_agent_client/models/dto/library.dart';

part 'library_page.g.dart';


@JsonSerializable()
class LibraryPageDto extends Object {

  @JsonKey(name: 'pageNo')
  int pageNo;

  @JsonKey(name: 'pageSize')
  int pageSize;

  @JsonKey(name: 'total')
  String total;

  @JsonKey(name: 'list')
  List<LibraryDto> list;

  LibraryPageDto(this.pageNo,this.pageSize,this.total,this.list,);

  factory LibraryPageDto.fromJson(Map<String, dynamic> srcJson) => _$LibraryPageDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$LibraryPageDtoToJson(this);

}