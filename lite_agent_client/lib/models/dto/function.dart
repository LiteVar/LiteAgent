import 'package:json_annotation/json_annotation.dart';

part 'function.g.dart';


@JsonSerializable()
class FunctionDto extends Object {

  @JsonKey(name: 'toolId', defaultValue: '')
  String toolId;

  @JsonKey(name: 'functionName', defaultValue: '')
  String functionName;

  @JsonKey(name: 'requestMethod', defaultValue: '')
  String requestMethod;

  @JsonKey(name: 'mode', defaultValue: 0)
  int mode;

  @JsonKey(name: 'protocol', defaultValue: '')
  String protocol;

  FunctionDto(this.toolId,this.functionName,this.requestMethod,this.mode,this.protocol,);

  factory FunctionDto.fromJson(Map<String, dynamic> srcJson) => _$FunctionDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$FunctionDtoToJson(this);

}


