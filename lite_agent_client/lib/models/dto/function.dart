import 'package:json_annotation/json_annotation.dart';

part 'function.g.dart';


@JsonSerializable()
class FunctionDto extends Object {

  @JsonKey(name: 'toolId')
  String? toolId;

  @JsonKey(name: 'functionName')
  String functionName;

  @JsonKey(name: 'requestMethod')
  String? requestMethod;

  @JsonKey(name: 'mode')
  int? mode;

  @JsonKey(name: 'protocol')
  String? protocol;

  FunctionDto(this.toolId,this.functionName,this.requestMethod,this.mode,this.protocol,);

  factory FunctionDto.fromJson(Map<String, dynamic> srcJson) => _$FunctionDtoFromJson(srcJson);

  Map<String, dynamic> toJson() => _$FunctionDtoToJson(this);

}


