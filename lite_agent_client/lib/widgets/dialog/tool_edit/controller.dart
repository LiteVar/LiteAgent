import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/tool/opentool_util.dart';
import 'package:lite_agent_client/utils/tool/tool_converter.dart';
import 'package:lite_agent_client/utils/extension/tool_extension.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:opentool_dart/opentool_dart.dart';

import '../../../models/dto/open_tool_schema.dart';

typedef ToolFormData = ({
  String name,
  String description,
  String schemaType,
  String schemaText,
  String apiType,
  String apiText,
  bool supportMultiAgent
});

class EditToolDialogController extends GetxController {
  static const List<String> apiTypeList = ['暂不选择', 'Bearer', 'Basic'];

  final TextEditingController nameController = TextEditingController();
  final TextEditingController desController = TextEditingController();
  final TextEditingController schemaTextController = TextEditingController();
  final TextEditingController apiTextController = TextEditingController();
  final TextEditingController serverApiKeyController = TextEditingController();
  final TextEditingController serverUrlController = TextEditingController();
  final TextEditingController openToolSchemaController = TextEditingController();
  final FocusNode openToolSchemaFocusNode = FocusNode();

  final supportAutoMultiAgents = false.obs;
  final selectSchemaType = Rx<String?>(null);
  final selectAPIType = Rx<String?>(null);
  final dataSource = "server".obs; // "server" or "input"
  final isServerFetching = false.obs;

  // 保存当前编辑的工具（用于验证名称唯一性时排除自己）
  ToolModel? _currentTool;

  void initData(ToolModel? tool) {
    _currentTool = tool;
    _resetForm();
    if (tool != null) {
      String apiType = tool.apiType;
      nameController.text = tool.name;
      desController.text = tool.description;
      selectSchemaType.value = tool.schemaType;
      String schemaText = tool.schemaText;
      if (tool.schemaType == ToolValidator.OPTION_OPENTOOL_SERVER) {
        _parseOpenToolSchema(schemaText);
      } else {
        schemaTextController.text = schemaText;
      }
      apiTextController.text = tool.apiText;

      if (apiType.isNotEmpty) {
        selectAPIType.value = ToolConverter.normalizeApiKeyType(apiType);
      } else {
        selectAPIType.value = apiTypeList.first;
      }
      supportAutoMultiAgents.value = tool.supportMultiAgent ?? false;
    }
  }

  void _resetForm() {
    nameController.clear();
    desController.clear();
    desController.clear();
    schemaTextController.clear();
    apiTextController.clear();
    serverApiKeyController.clear();
    serverUrlController.clear();
    openToolSchemaController.clear();

    selectSchemaType.value = null;
    selectAPIType.value = apiTypeList.first;
    supportAutoMultiAgents.value = false;
    dataSource.value = "server";
    isServerFetching.value = false;
  }

  @override
  void onClose() {
    nameController.dispose();
    desController.dispose();
    schemaTextController.dispose();
    apiTextController.dispose();
    serverApiKeyController.dispose();
    serverUrlController.dispose();
    openToolSchemaController.dispose();
    openToolSchemaFocusNode.dispose();
    super.onClose();
  }

  Future<void> exportToolData(bool exportPlaintext) async {
    final toolData = await getFormDataAndValidate();
    if (toolData != null) {
      final int dtoSchemaType = ToolConverter.getDtoProtocol(toolData.schemaType);
      final ToolDTO toolDTO = ToolDTO("", toolData.name, toolData.description, dtoSchemaType, toolData.schemaText, toolData.apiText,
          toolData.apiType.toLowerCase(), false, toolData.supportMultiAgent, null, "", 0);

      String? savePath = await toolDTO.exportJson(exportPlaintext);
      bool isSuccess = savePath?.isNotEmpty == true;
      AlarmUtil.showAlertToast(isSuccess ? "工具数据导出成功！" : "导出失败");
    }
  }

  Future<void> fetchFromServer() async {
    String serverUrl = serverUrlController.text.trimmed();
    String apiKey = serverApiKeyController.text.trimmed();
    if (serverUrl.isEmpty) {
      AlarmUtil.showAlertDialog("请输入服务器地址");
      return;
    }
    isServerFetching.value = true;
    try {
      Uri? uri = Uri.tryParse(serverUrl);
      if (uri == null || (!uri.hasScheme || (uri.scheme != 'http' && uri.scheme != 'https')) || uri.host.isEmpty) {
        AlarmUtil.showAlertDialog("服务器地址格式不正确");
        return;
      }

      if (!serverUrl.contains('://')) {
        uri = Uri.tryParse('http://$serverUrl');
      }
      if (uri == null) {
        AlarmUtil.showAlertDialog("服务器地址解析失败");
        return;
      }

      final bool isSSL = (uri.scheme == 'https');
      final String host = uri.host.isNotEmpty ? uri.host : uri.path;
      final int port = uri.port != 0 ? uri.port : (isSSL ? 443 : 80);

      final OpenTool? tool = await OpenToolFetcher.loadByAddress(isSSL: isSSL, host: host, port: port, apiKey: apiKey);
      if (tool == null) {
        openToolSchemaController.text = "获取内容为空";
        AlarmUtil.showAlertToast("获取 Schema 为空，请检查服务器地址和 APIKey");
      } else {
        openToolSchemaController.text = json.encode(tool.toJson());
      }
    } catch (e) {
      AlarmUtil.showAlertDialog("获取数据失败：${e.toString()}");
    } finally {
      isServerFetching.value = false;
    }
  }

  Future<ToolFormData?> getFormDataAndValidate() async {
    final toolData = getFormData();
    final errorString = await validateFormData(toolData);
    if (errorString.isNotEmpty) {
      AlarmUtil.showAlertDialog(errorString);
      return null;
    }
    return toolData;
  }

  Future<String> validateFormData(ToolFormData formData) async {
    String name = formData.name;
    String schemaText = formData.schemaText;
    String schemaType = formData.schemaType;

    if (name.isEmpty) {
      return "工具名称不能为空";
    }
    if (!await ToolValidator.isNameUniqueAsync(name, excludeId: _currentTool?.id)) {
      return "工具名称已存在，请重新输入";
    }
    if ((schemaText.isEmpty || formData.schemaType.isEmpty)) {
      AlarmUtil.showAlertDialog("类型和文稿不能为空");
      return "类型和文稿不能为空";
    }
    if (schemaText.isNotEmpty && !(await ToolValidator.validateSchemaFormat(schemaType, schemaText))) {
      return "Schema解析失败";
    }
    if (schemaType == ToolValidator.OPTION_OPENTOOL_SERVER) {
      String serverUrl = serverUrlController.text.trim();
      if (serverUrl.isEmpty) {
        return "OpenTool服务器地址不能为空";
      }
      Uri? uri = Uri.tryParse(serverUrl);
      if (uri == null || (!uri.hasScheme || (uri.scheme != 'http' && uri.scheme != 'https')) || uri.host.isEmpty) {
        return "OpenTool服务器地址格式不正确";
      }
    }
    return "";
  }

  ToolFormData getFormData() {
    String name = nameController.text.trim();
    String description = desController.text.trim();
    String schemaType = selectSchemaType.value ?? "";
    late String schemaText;
    if (schemaType == ToolValidator.OPTION_OPENTOOL_SERVER) {
      final dto = OpenToolSchemaDTO(
          dataSource.value, serverApiKeyController.text.trim(), serverUrlController.text.trim(), openToolSchemaController.text.trim());
      schemaText = json.encode(dto.toJson());
    } else {
      schemaText = schemaTextController.text.trim();
    }
    String apiType = "";
    String apiText = "";

    if (schemaType == Protocol.OPENAPI || schemaType == Protocol.JSONRPCHTTP) {
      if (selectAPIType.value == "Bearer" || selectAPIType.value == "Basic") {
        apiType = selectAPIType.value ?? "";
      }
      apiText = apiTextController.text.trim();
    }

    bool supportAutoMultiAgent = supportAutoMultiAgents.value;

    return (
      name: name,
      description: description,
      schemaType: schemaType,
      schemaText: schemaText,
      apiType: apiType,
      apiText: apiText,
      supportMultiAgent: supportAutoMultiAgent,
    );
  }

  void onDataSourceChanged(String value) {
    dataSource.value = value;
    openToolSchemaFocusNode.removeListener(_handleSchemaFocusChange);
    if (value == "input") {
      openToolSchemaFocusNode.addListener(_handleSchemaFocusChange);
    }
  }

  void _handleSchemaFocusChange() {
    if (!openToolSchemaFocusNode.hasFocus && dataSource.value == "input") {
      String openToolSchemaString = openToolSchemaController.text.trim();
      try {
        OpenTool openTool = OpenTool.fromJson(json.decode(openToolSchemaString));
        String? serverUrl = openTool.server?.url;
        if (serverUrl != null && serverUrl.isNotEmpty) {
          serverUrlController.text = serverUrl;
        }
      } catch (e) {
        Log.e("解析OpenToolSchema失败：$e");
      }
    }
  }

  void _parseOpenToolSchema(String schemaText) {
    if (schemaText.isNotEmpty && schemaText.isJson()) {
      try {
        Map<String, dynamic> schemaMap = json.decode(schemaText);
        OpenToolSchemaDTO openToolSchema = OpenToolSchemaDTO.fromJson(schemaMap);

        dataSource.value = openToolSchema.origin != "input" ? "server" : "input";
        if (openToolSchema.schema.isNotEmpty) {
          openToolSchemaController.text = openToolSchema.schema;
        }
        if (openToolSchema.apiKey.isNotEmpty) {
          serverApiKeyController.text = openToolSchema.apiKey;
        }
        if (openToolSchema.serverUrl.isNotEmpty) {
          serverUrlController.text = openToolSchema.serverUrl;
        }
      } catch (e) {
        Log.e("解析OpenToolSchema失败：$e");
      }
    }
  }
}
