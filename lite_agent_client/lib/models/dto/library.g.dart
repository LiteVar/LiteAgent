// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'library.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LibraryDto _$LibraryDtoFromJson(Map<String, dynamic> json) => LibraryDto(
      json['id'] as String? ?? '',
      json['name'] as String? ?? '',
      json['icon'] as String? ?? '',
      json['description'] as String? ?? '',
      json['shareFlag'] as bool? ?? false,
      json['dataSourceType'] as String? ?? '',
      (json['docCount'] as num?)?.toInt() ?? 0,
      (json['wordCount'] as num?)?.toInt() ?? 0,
      (json['agentCount'] as num?)?.toInt() ?? 0,
      json['embeddingModelId'] as String?,
      json['llmModelId'] as String?,
      json['similarId'] as String?,
      (json['operate'] as num?)?.toInt(),
    );

Map<String, dynamic> _$LibraryDtoToJson(LibraryDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'icon': instance.icon,
      'description': instance.description,
      'shareFlag': instance.shareFlag,
      'dataSourceType': instance.dataSourceType,
      'docCount': instance.docCount,
      'wordCount': instance.wordCount,
      'agentCount': instance.agentCount,
      'embeddingModelId': instance.embeddingModelId,
      'llmModelId': instance.llmModelId,
      'similarId': instance.similarId,
      'operate': instance.operate,
    };
