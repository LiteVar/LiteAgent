import 'package:json_annotation/json_annotation.dart';

part 'chat.g.dart';


@JsonSerializable()
class ChatDTO extends Object {

  @JsonKey(name: 'agentId')
  String agentId;

  @JsonKey(name: 'sessionIds')
  List<String> sessionIds;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'createTime')
  String createTime;

  ChatDTO(this.agentId,this.sessionIds,this.name,this.createTime,);

  factory ChatDTO.fromJson(Map<String, dynamic> srcJson) => _$ChatDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ChatDTOToJson(this);

}