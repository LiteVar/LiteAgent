// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'function.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

FunctionDto _$FunctionDtoFromJson(Map<String, dynamic> json) => FunctionDto(
      json['toolId'] as String?,
      json['functionName'] as String,
      json['requestMethod'] as String?,
      (json['mode'] as num?)?.toInt(),
      json['protocol'] as String?,
    );

Map<String, dynamic> _$FunctionDtoToJson(FunctionDto instance) =>
    <String, dynamic>{
      'toolId': instance.toolId,
      'functionName': instance.functionName,
      'requestMethod': instance.requestMethod,
      'mode': instance.mode,
      'protocol': instance.protocol,
    };
