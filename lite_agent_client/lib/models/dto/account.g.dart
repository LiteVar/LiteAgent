// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'account.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

AccountDTO _$AccountDTOFromJson(Map<String, dynamic> json) => AccountDTO(
      json['id'] as String,
      json['name'] as String,
      json['email'] as String,
      json['avatar'] as String?,
      (json['status'] as num?)?.toInt(),
      json['createTime'] as String?,
      json['updateTime'] as String?,
    );

Map<String, dynamic> _$AccountDTOToJson(AccountDTO instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'email': instance.email,
      'avatar': instance.avatar,
      'status': instance.status,
      'createTime': instance.createTime,
      'updateTime': instance.updateTime,
    };
