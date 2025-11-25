import 'package:json_annotation/json_annotation.dart';

part 'open_tool_schema.g.dart';

@JsonSerializable()
class OpenToolSchemaDTO extends Object {
  @JsonKey(name: 'origin', defaultValue: '')
  String origin; //server/input

  @JsonKey(name: 'apiKey', defaultValue: '')
  String apiKey;

  @JsonKey(name: 'serverUrl', defaultValue: '')
  String serverUrl;

  @JsonKey(name: 'schema', defaultValue: '')
  String schema;

  OpenToolSchemaDTO(this.origin, this.apiKey, this.serverUrl, this.schema);

  factory OpenToolSchemaDTO.fromJson(Map<String, dynamic> srcJson) => _$OpenToolSchemaDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$OpenToolSchemaDTOToJson(this);
}
