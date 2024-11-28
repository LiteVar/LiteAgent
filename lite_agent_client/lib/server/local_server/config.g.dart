// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'config.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Config _$ConfigFromJson(Map<String, dynamic> json) => Config(
      version: json['version'] as String,
      server: Server.fromJson(json['server'] as Map<String, dynamic>),
      log: Log.fromJson(json['log'] as Map<String, dynamic>),
    );

Server _$ServerFromJson(Map<String, dynamic> json) => Server(
      ip: json['ip'] as String? ?? "127.0.0.1",
      apiPathPrefix: json['apiPathPrefix'] as String? ?? "/api",
      port: (json['port'] as num?)?.toInt() ?? 9527,
    );
