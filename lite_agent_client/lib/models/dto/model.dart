import 'package:json_annotation/json_annotation.dart';

part 'model.g.dart';

@JsonSerializable()
class ModelDTO extends Object {
  @JsonKey(name: 'id')
  String id;

  @JsonKey(name: 'alias')
  String alias;

  @JsonKey(name: 'name')
  String name;

  @JsonKey(name: 'baseUrl')
  String baseUrl;

  @JsonKey(name: 'apiKey')
  String apiKey;

  @JsonKey(name: 'maxTokens')
  int? maxTokens;

  //LLM，embedding,asr，tts，image
  @JsonKey(name: 'type')
  String type;

  @JsonKey(name: 'autoAgent')
  bool? autoAgent;

  @JsonKey(name: 'toolInvoke')
  bool? toolInvoke;

  @JsonKey(name: 'deepThink')
  bool? deepThink;

  ModelDTO(this.id, this.alias, this.name, this.baseUrl, this.apiKey, this.maxTokens, this.type, this.autoAgent, this.toolInvoke,
      this.deepThink);

  factory ModelDTO.fromJson(Map<String, dynamic> srcJson) => _$ModelDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ModelDTOToJson(this);
}
