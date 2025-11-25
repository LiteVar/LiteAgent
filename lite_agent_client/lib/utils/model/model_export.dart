import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';

class ModelExportUtil {
  static Future<String?> exportModelToJson(ModelDTO modelDTO, {bool exportPlaintext = false}) async {
    try {
      final exportData = buildModelExportData(modelDTO, exportPlaintext: exportPlaintext);
      final fileName = _generateFileName(modelDTO);

      final savePath = await FilePicker.platform
          .saveFile(dialogTitle: '选择保存位置', fileName: fileName, type: FileType.custom, allowedExtensions: ['json']);

      if (savePath != null) {
        await File(savePath).writeAsString(const JsonEncoder.withIndent('  ').convert(exportData), encoding: utf8);
        Log.i("模型数据导出成功: $savePath");
      }

      return savePath;
    } catch (e) {
      Log.e("导出模型数据失败", e);
      return null;
    }
  }

  static Map<String, dynamic> buildModelExportData(ModelDTO modelDTO, {bool exportPlaintext = false}) {
    return {
      "name": modelDTO.name,
      "alias": modelDTO.alias,
      "baseUrl": exportPlaintext ? modelDTO.baseUrl : "{{<ENDPOINT>}}",
      "apiKey": exportPlaintext ? modelDTO.apiKey : "{{<APIKEY>}}",
      "type": modelDTO.type,
      "maxTokens": modelDTO.maxTokens,
      "autoAgent": modelDTO.autoAgent,
      "toolInvoke": modelDTO.toolInvoke,
      "deepThink": modelDTO.deepThink,
    };
  }

  static String _generateFileName(ModelDTO modelDTO) {
    final fileName = modelDTO.alias.isEmpty ? "  模型_${modelDTO.id.lastSixChars}" : modelDTO.alias;
    return "$fileName.json";
  }
}

