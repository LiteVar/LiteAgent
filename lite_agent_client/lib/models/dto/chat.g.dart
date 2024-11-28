// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'chat.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ChatDTO _$ChatDTOFromJson(Map<String, dynamic> json) => ChatDTO(
      json['agentId'] as String,
      (json['sessionIds'] as List<dynamic>).map((e) => e as String).toList(),
      json['name'] as String,
      json['createTime'] as String,
    );

Map<String, dynamic> _$ChatDTOToJson(ChatDTO instance) => <String, dynamic>{
      'agentId': instance.agentId,
      'sessionIds': instance.sessionIds,
      'name': instance.name,
      'createTime': instance.createTime,
    };
