import 'package:json_annotation/json_annotation.dart';

part 'model.g.dart';

@JsonSerializable()
class ModelDTO extends Object {
  @JsonKey(name: 'id', defaultValue: '')
  String id;

  @JsonKey(name: 'alias', defaultValue: '')
  String alias;

  @JsonKey(name: 'name', defaultValue: '')
  String name;

  @JsonKey(name: 'baseUrl', defaultValue: '')
  String baseUrl;

  @JsonKey(name: 'apiKey', defaultValue: '')
  String apiKey;

  @JsonKey(name: 'maxTokens', defaultValue: 4096)
  int maxTokens;

  //LLM，embedding,asr，tts，image
  @JsonKey(name: 'type', defaultValue: 'LLM')
  String type;

  @JsonKey(name: 'autoAgent', defaultValue: false)
  bool autoAgent;

  @JsonKey(name: 'toolInvoke', defaultValue: false)
  bool toolInvoke;

  @JsonKey(name: 'deepThink', defaultValue: false)
  bool deepThink;

  ///导入时发现的重名模型id
  @JsonKey(name: 'similarId', defaultValue: "")
  String similarId;

  ///导入时对重名模型的操作
  @JsonKey(name: 'operate', defaultValue: 0)
  int operate;

  ModelDTO(this.id, this.alias, this.name, this.baseUrl, this.apiKey, this.maxTokens, this.type, this.autoAgent, this.toolInvoke,
      this.deepThink, this.similarId, this.operate);

  factory ModelDTO.fromJson(Map<String, dynamic> srcJson) => _$ModelDTOFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ModelDTOToJson(this);
}
