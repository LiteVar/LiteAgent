import 'package:json_annotation/json_annotation.dart';

part 'account.g.dart';


@JsonSerializable()
class AccountDTO extends Object {

  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'name', defaultValue: '')
  String name;

  @JsonKey(name: 'email', defaultValue: '')
  String email;

  @JsonKey(name: 'avatar', defaultValue: '')
  String avatar;

  @JsonKey(name: 'status', defaultValue: 0)
  int status;

  @JsonKey(name: 'createTime', defaultValue: '')
  String createTime;

  @JsonKey(name: 'updateTime', defaultValue: '')
  String updateTime;

  AccountDTO(this.id,this.name,this.email,this.avatar,this.status,this.createTime,this.updateTime,);

  factory AccountDTO.fromJson(Map<String, dynamic> srcJson) => _$AccountDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AccountDTOToJson(this);

}


