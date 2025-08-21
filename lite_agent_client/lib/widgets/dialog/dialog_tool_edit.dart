import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';

class SchemaType {
  static const String OPENAPI = "OpenAPI3(YAML/JSON)";
  static const String OPENMODBUS = "OpenModbus(JSON)";
  static const String JSONRPCHTTP = "OpenRPC(JSON)";
  static const String OPENTOOL = "第三方OpenTool";
  static const String MCP_STDIO_TOOLS = "MCP(stdio)";
}

class EditToolDialog extends StatelessWidget {
  ToolBean? tool;
  late String name;
  late String description;
  late String schemaType;
  late String schemaText;
  late String apiType;
  late String apiText;
  late bool supportMultiAgent;
  late bool isEdit;
  void Function(
          String name, String description, String schemaType, String schemaText, String apiType, String apiText, bool supportMultiAgent)
      onConfirmCallback;

  EditToolDialog({super.key, required this.tool, required this.isEdit, required this.onConfirmCallback}) {
    name = tool?.name ?? "";
    description = tool?.description ?? "";
    schemaType = tool?.schemaType ?? "";
    if (schemaType == SchemaType.OPENAPI) {
      schemaType = Protocol.OPENAPI;
    } else if (schemaType == SchemaType.JSONRPCHTTP) {
      schemaType = Protocol.JSONRPCHTTP;
    } else if (schemaType == SchemaType.OPENMODBUS) {
      schemaType = Protocol.OPENMODBUS;
    }
    schemaText = tool?.schemaText ?? "";
    apiType = tool?.apiType ?? "";
    apiText = tool?.apiText ?? "";
    supportMultiAgent = tool?.supportMultiAgent ?? false;
  }

  final schemaTypeList = <String>[Protocol.OPENAPI, Protocol.JSONRPCHTTP, Protocol.OPENMODBUS, Protocol.OPENTOOL, Protocol.MCP_STDIO_TOOLS];

  final itemBorderColor = const Color(0xFFd9d9d9);
  final logic = Get.put(EditToolDialogController());

  Future<void> _confirm() async {
    String name = logic.nameController.text;
    String description = logic.desController.text;
    String schemaType = logic.selectSchemaType.value ?? "";
    String schemaText = logic.schemaTextController.text;
    String apiType = "";
    String apiText = "";
    if (schemaType == SchemaType.OPENAPI || schemaType == SchemaType.JSONRPCHTTP) {
      if (logic.selectAPIType.value == "Bearer" || logic.selectAPIType.value == "Basic") {
        apiType = logic.selectAPIType.value ?? "";
      }
      apiText = logic.apiTextController.text;
    }
    if (name.isEmpty) {
      AlarmUtil.showAlertDialog("工具名称不能为空");
      return;
    }
    if ((schemaText.isEmpty || schemaType.isEmpty)) {
      AlarmUtil.showAlertDialog("类型和文稿不能为空");
      return;
    }
    if (schemaText.isNotEmpty && !(await isSchemaTextCorrect(schemaType, schemaText))) {
      AlarmUtil.showAlertDialog("Schema解析失败");
      return;
    }
    onConfirmCallback(name, description, schemaType, schemaText, apiType, apiText, logic.supportAutoMultiAgents.value);
    Get.back();
  }

  Future<bool> isSchemaTextCorrect(String schemaType, String schemaText) async {
    if (schemaType == Protocol.OPENAPI) {
      if ((await schemaText.isOpenAIJson()) || await schemaText.isOpenAIYaml()) {
        return true;
      }
    } else if (schemaType == Protocol.OPENMODBUS) {
      if ((await schemaText.isOpenModBusJson())) {
        return true;
      }
    } else if (schemaType == Protocol.JSONRPCHTTP) {
      if ((await schemaText.isOpenPPCJson())) {
        return true;
      }
    } else if (schemaType == Protocol.OPENTOOL) {
      if ((await schemaText.isOpenToolJson())) {
        return true;
      }
    } else if (schemaType == Protocol.MCP_STDIO_TOOLS) {
      if ((schemaText.isMCPServersJson())) {
        return true;
      }
    }
    return false;
  }

  @override
  Widget build(BuildContext context) {
    logic.initData(name, description, schemaType, schemaText, apiType, apiText, supportMultiAgent);
    return Center(
        child: Container(
            width: 538,
            height: 538,
            decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
            child: Column(children: [
              buildTitleContainer(),
              Expanded(
                  child: SingleChildScrollView(
                      child: Container(
                          margin: const EdgeInsets.all(16),
                          child: Column(children: [
                            buildToolDesColumn(),
                            buildSchemaInputColumn(),
                            //buildMCPInputColumn(),
                            Obx(() => Offstage(
                                  offstage: !(logic.selectSchemaType.value == Protocol.OPENAPI ||
                                      logic.selectSchemaType.value == Protocol.JSONRPCHTTP),
                                  child: buildAPIInputColumn(),
                                )),
                            buildOtherSettingColumn(),
                            const SizedBox(height: 10),
                            buildBottomButton()
                          ])))),
            ])));
  }

  Row buildBottomButton() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)))),
            onPressed: () {
              Get.back();
            },
            child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14))),
        const SizedBox(width: 16),
        TextButton(
            style: ButtonStyle(
              padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
              backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
              shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(2))),
            ),
            onPressed: () {
              _confirm();
            }.throttle(),
            child: const Text('确定', style: TextStyle(color: Colors.white, fontSize: 14)))
      ],
    );
  }

  Column buildOtherSettingColumn() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("其他", style: TextStyle(fontSize: 14, color: Colors.black)),
        Container(margin: const EdgeInsets.fromLTRB(0, 18, 0, 12), child: const Divider(height: 0.1)),
        Container(
            margin: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text("是否支持Auto Multi Agent使用", style: TextStyle(fontSize: 14, color: Colors.black)),
              Container(
                margin: const EdgeInsets.symmetric(vertical: 12),
                child: Obx(() => Row(
                      children: [
                        Radio<bool>(
                          activeColor: Colors.blue,
                          value: true,
                          groupValue: logic.supportAutoMultiAgents.value,
                          onChanged: (value) => logic.supportAutoMultiAgents.value = value!,
                          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          visualDensity:
                              const VisualDensity(horizontal: VisualDensity.minimumDensity, vertical: VisualDensity.minimumDensity),
                          splashRadius: 0,
                        ),
                        const Text("是", style: TextStyle(fontSize: 14)),
                        const SizedBox(width: 40),
                        Radio<bool>(
                          activeColor: Colors.blue,
                          value: false,
                          groupValue: logic.supportAutoMultiAgents.value,
                          onChanged: (value) => logic.supportAutoMultiAgents.value = value!,
                          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          visualDensity:
                              const VisualDensity(horizontal: VisualDensity.minimumDensity, vertical: VisualDensity.minimumDensity),
                          splashRadius: 0,
                        ),
                        const Text("否", style: TextStyle(fontSize: 14)),
                      ],
                    )),
              ),
            ]))
      ],
    );
  }

  Column buildAPIInputColumn() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("API Key", style: TextStyle(fontSize: 14, color: Colors.black)),
        Container(margin: const EdgeInsets.fromLTRB(0, 18, 0, 12), child: const Divider(height: 0.1)),
        Container(
            margin: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text("认证类型", style: TextStyle(fontSize: 14, color: Colors.black)),
              Container(
                  height: 36,
                  margin: const EdgeInsets.symmetric(vertical: 8),
                  decoration:
                      BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: const BorderRadius.all(Radius.circular(4))),
                  child: Center(
                      child: Obx(() => DropdownButtonHideUnderline(
                            child: DropdownButton2(
                              isExpanded: true,
                              items: logic.apiTypeList
                                  .map<DropdownMenuItem<String>>((String item) =>
                                      DropdownMenuItem<String>(value: item, child: Text(item, style: const TextStyle(fontSize: 14))))
                                  .toList(),
                              value: logic.selectAPIType.value,
                              hint: const Text("这里显示key类型", style: TextStyle(fontSize: 14)),
                              onChanged: (value) {
                                logic.selectAPIType.value = value;
                              },
                              dropdownStyleData: const DropdownStyleData(
                                  offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
                            ),
                          )))),
              const SizedBox(height: 10),
              buildInputWidget(title: "Key值", itemHeight: 75, hint: "请输入API Key", controller: logic.apiTextController),
            ]))
      ],
    );
  }

  Column buildSchemaInputColumn() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("Schema", style: TextStyle(fontSize: 14, color: Colors.black)),
        Container(margin: const EdgeInsets.fromLTRB(0, 18, 0, 12), child: const Divider(height: 0.1)),
        Container(
            margin: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                const Text("类型", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
                Container(
                    margin: const EdgeInsets.only(left: 10), child: const Text("*", style: TextStyle(fontSize: 14, color: Colors.red)))
              ]),
              Container(
                  height: 36,
                  margin: const EdgeInsets.symmetric(vertical: 8),
                  decoration:
                      BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: const BorderRadius.all(Radius.circular(4))),
                  child: Center(
                      child: Obx(() => DropdownButtonHideUnderline(
                            child: DropdownButton2(
                              isExpanded: true,
                              items: schemaTypeList
                                  .map<DropdownMenuItem<String>>((String item) => DropdownMenuItem<String>(
                                      value: item, child: Text(getSchemaTypeOptionString(item), style: const TextStyle(fontSize: 14))))
                                  .toList(),
                              value: logic.selectSchemaType.value,
                              hint: const Text("这里显示协议类型", style: TextStyle(fontSize: 14, color: Color(0x40000000))),
                              onChanged: (value) => logic.selectSchemaType.value = value,
                              dropdownStyleData: const DropdownStyleData(
                                  offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
                            ),
                          )))),
              const SizedBox(height: 10),
              buildInputWidget(title: "文稿", isRequired: true, itemHeight: 175, hint: "请输入schema文稿", controller: logic.schemaTextController),
            ]))
      ],
    );
  }

  String getSchemaTypeOptionString(String type) {
    return switch (type) {
      Protocol.OPENAPI => SchemaType.OPENAPI,
      Protocol.OPENMODBUS => SchemaType.OPENMODBUS,
      Protocol.JSONRPCHTTP => SchemaType.JSONRPCHTTP,
      Protocol.OPENTOOL => SchemaType.OPENTOOL,
      Protocol.MCP_STDIO_TOOLS => SchemaType.MCP_STDIO_TOOLS,
      String() => "",
    };
  }

  Column buildInputWidget(
      {required String title,
      bool isRequired = false,
      required double itemHeight,
      int? maxLines,
      String? hint,
      required TextEditingController controller}) {
    var singleLine = maxLines == 1;
    var textField = TextField(
        controller: controller,
        cursorColor: Colors.blue,
        maxLines: maxLines,
        decoration: InputDecoration(
            hintStyle: const TextStyle(color: Color(0x40000000)),
            hintText: hint,
            border: InputBorder.none,
            isDense: singleLine,
            contentPadding: const EdgeInsets.symmetric(horizontal: 8)),
        style: const TextStyle(fontSize: 14, color: Color(0xff333333)));
    return Column(children: [
      Row(children: [
        Text(title, style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
        if (isRequired)
          Container(margin: const EdgeInsets.only(left: 10), child: const Text("*", style: TextStyle(fontSize: 14, color: Colors.red)))
      ]),
      Container(
          height: itemHeight,
          margin: const EdgeInsets.symmetric(vertical: 15),
          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
          decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(2)),
          child: singleLine ? Center(child: textField) : textField)
    ]);
  }

  Column buildToolDesColumn() {
    return Column(children: [
      buildInputWidget(title: "工具名称", isRequired: true, itemHeight: 40, maxLines: 1, hint: "请输入工具名称", controller: logic.nameController),
      buildInputWidget(title: "描述", itemHeight: 82, hint: "用简单几句话将工具介绍给用户", controller: logic.desController),
    ]);
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
          color: Color(0xFFf5f5f5), borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6))),
      child: Row(children: [
        Text(isEdit ? "编辑工具" : "新建工具"),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () => Get.back(),
        )
      ]),
    );
  }
}

class EditToolDialogController extends GetxController {
  final supportAutoMultiAgents = false.obs;
  final apiTypeList = <String>['暂不选择', 'Bearer', 'Basic'];

  final TextEditingController nameController = TextEditingController();
  final TextEditingController desController = TextEditingController();
  final TextEditingController schemaTextController = TextEditingController();
  final TextEditingController apiTextController = TextEditingController();

  var selectSchemaType = Rx<String?>(null);
  var selectAPIType = Rx<String?>(null);

  void initData(
      String name, String description, String schemaType, String schemaText, String apiType, String apiText, bool supportMultiAgents) {
    nameController.text = name;
    desController.text = description;
    schemaTextController.text = schemaText;
    selectSchemaType.value = null;
    if (schemaType.isNotEmpty) {
      switch (schemaType) {
        case SchemaType.OPENAPI:
          selectSchemaType.value = Protocol.OPENAPI;
          break;
        case SchemaType.OPENMODBUS:
          selectSchemaType.value = Protocol.OPENMODBUS;
          break;
        case SchemaType.JSONRPCHTTP:
          selectSchemaType.value = Protocol.JSONRPCHTTP;
          break;
        default:
          selectSchemaType.value = schemaType;
          break;
      }
    }
    apiTextController.text = apiText;
    if (apiType.isNotEmpty) {
      selectAPIType.value = apiType;
    } else {
      selectAPIType.value = apiTypeList.first;
    }
    supportAutoMultiAgents.value = supportMultiAgents;
  }

  @override
  void onClose() {
    nameController.dispose();
    desController.dispose();
    schemaTextController.dispose();
    apiTextController.dispose();
    super.onClose();
  }
}
