import 'package:json_annotation/json_annotation.dart';

part 'workspace.g.dart';

@JsonSerializable()
class WorkSpaceDTO extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'name', defaultValue: '')
  String name;

  @JsonKey(name: 'role', defaultValue: 0)
  int role;

  WorkSpaceDTO(this.id, this.name, this.role);

  factory WorkSpaceDTO.fromJson(Map<String, dynamic> srcJson) => _$WorkSpaceDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$WorkSpaceDTOToJson(this);
}
