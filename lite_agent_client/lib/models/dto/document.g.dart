// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'document.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DocumentDto _$DocumentDtoFromJson(Map<String, dynamic> json) => DocumentDto(
      json['id'] as String? ?? '',
      json['name'] as String? ?? '',
      json['datasetId'] as String? ?? '',
      json['dataSourceType'] as String? ?? '',
      json['content'] as String? ?? '',
      (json['htmlUrl'] as List<dynamic>?)?.map((e) => e as String).toList(),
      (json['wordCount'] as num?)?.toInt() ?? 0,
      (json['tokenCount'] as num?)?.toInt() ?? 0,
      json['enableFlag'] as bool? ?? true,
      json['fileId'] as String? ?? '',
    );

Map<String, dynamic> _$DocumentDtoToJson(DocumentDto instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'datasetId': instance.datasetId,
      'dataSourceType': instance.dataSourceType,
      'content': instance.content,
      'htmlUrl': instance.htmlUrl,
      'wordCount': instance.wordCount,
      'tokenCount': instance.tokenCount,
      'enableFlag': instance.enableFlag,
      'fileId': instance.fileId,
    };
