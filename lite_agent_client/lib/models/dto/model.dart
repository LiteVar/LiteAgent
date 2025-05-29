import 'package:json_annotation/json_annotation.dart';

part 'model.g.dart';

@JsonSerializable()
class ModelDTO extends Object {
  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'baseUrl')
  String baseUrl;

  @JsonKey(name: 'apiKey')
  String apiKey;

  ModelDTO(this.id, this.name, this.baseUrl, this.apiKey);

  factory ModelDTO.fromJson(Map<String, dynamic> srcJson) => _$ModelDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ModelDTOToJson(this);
}
