import 'package:json_annotation/json_annotation.dart';

part 'workspace.g.dart';

List<WorkSpaceDTO> getWorkspaceDTOList(List<dynamic> list) {
  List<WorkSpaceDTO> result = [];
  for (var item in list) {
    result.add(WorkSpaceDTO.fromJson(item));
  }
  return result;
}

@JsonSerializable()
class WorkSpaceDTO extends Object {
  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'role')
  int role;

  WorkSpaceDTO(
    this.id,
    this.name,
    this.role,
  );

  factory WorkSpaceDTO.fromJson(Map<String, dynamic> srcJson) => _$WorkSpaceDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$WorkSpaceDTOToJson(this);
}
