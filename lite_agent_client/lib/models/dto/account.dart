import 'package:json_annotation/json_annotation.dart';

part 'account.g.dart';


@JsonSerializable()
class AccountDTO extends Object {

  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'email')
  String email;

  @JsonKey(name: 'avatar')
  String? avatar;

  @JsonKey(name: 'status')
  int? status;

  @JsonKey(name: 'createTime')
  String? createTime;

  @JsonKey(name: 'updateTime')
  String? updateTime;

  AccountDTO(this.id,this.name,this.email,this.avatar,this.status,this.createTime,this.updateTime,);

  factory AccountDTO.fromJson(Map<String, dynamic> srcJson) => _$AccountDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$AccountDTOToJson(this);

}


