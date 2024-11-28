// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ModelDTO _$ModelDTOFromJson(Map<String, dynamic> json) => ModelDTO(
      json['id'] as String,
      json['name'] as String,
      json['baseUrl'] as String,
      json['apiKey'] as String,
      json['userId'] as String,
      json['workspaceId'] as String,
      json['shareFlag'] as bool,
      json['createTime'] as String,
      json['updateTime'] as String,
    );

Map<String, dynamic> _$ModelDTOToJson(ModelDTO instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'baseUrl': instance.baseUrl,
      'apiKey': instance.apiKey,
      'userId': instance.userId,
      'workspaceId': instance.workspaceId,
      'shareFlag': instance.shareFlag,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
