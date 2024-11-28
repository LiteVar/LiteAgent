import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';

class SchemaType {
  static const String openapi = "OpenAPI3(YAML/JSON)";
  static const String openmodbus = "OpenModbus(JSON)";
  static const String jsonrpcHttp = "OpenRPC(JSON)";
}

class EditToolDialog extends StatefulWidget {
  ToolBean? tool;
  late String name;
  late String description;
  late String schemaType;
  late String schemaText;
  late String apiType;
  late String apiText;
  late bool isEdit;
  void Function(String name, String description, String schemaType, String schemaText, String apiType, String apiText) onConfirmCallback;

  EditToolDialog({super.key, required this.tool, required this.isEdit, required this.onConfirmCallback}) {
    name = tool?.name ?? "";
    description = tool?.description ?? "";
    schemaType = tool?.schemaType ?? "";
    if (schemaType == Protocol.openapi) {
      schemaType = SchemaType.openapi;
    } else if (schemaType == Protocol.jsonrpcHttp) {
      schemaType = SchemaType.jsonrpcHttp;
    } else if (schemaType == Protocol.openmodbus) {
      schemaType = SchemaType.openmodbus;
    }
    schemaText = tool?.schemaText ?? "";
    apiType = tool?.apiType ?? "";
    if (apiType == "Bearer") {
      apiType == "bearer";
    } else if (apiType == "Basic") {
      apiType == "basic";
    }
    apiText = tool?.apiText ?? "";
  }

  @override
  State<EditToolDialog> createState() {
    return _EditToolDialogState();
  }
}

class _EditToolDialogState extends State<EditToolDialog> {
  final schemaTypeList = <String>[SchemaType.openapi, SchemaType.jsonrpcHttp, SchemaType.openmodbus];
  final apiTypeList = <String>['Bearer', 'Basic'];

  final itemBorderColor = const Color(0xFFd9d9d9);

  late TextEditingController _nameController;
  late TextEditingController _desController;
  late TextEditingController _schemaTextController;
  late TextEditingController _apiTextController;

  bool _needInit = true;
  String? _selectSchemaType;
  String? _selectAPIType;

  void initData() {
    _nameController = TextEditingController(text: widget.name);
    _desController = TextEditingController(text: widget.description);
    _schemaTextController = TextEditingController(text: widget.schemaText);
    if (widget.schemaType.isNotEmpty) {
      _selectSchemaType = widget.schemaType;
    }
    _apiTextController = TextEditingController(text: widget.apiText);
    if (widget.apiType.isNotEmpty) {
      _selectAPIType = widget.apiType;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _desController.dispose();
    _schemaTextController.dispose();
    _apiTextController.dispose();
    widget.tool = null;
    super.dispose();
  }

  void _confirm() {
    String name = _nameController.text;
    String description = _desController.text;
    String schemaType = _selectSchemaType ?? "";
    String schemaText = _schemaTextController.text;
    String apiType = _selectAPIType ?? "";
    String apiText = _apiTextController.text;
    if (name.isEmpty || schemaText.isEmpty || schemaType.isEmpty || apiText.isEmpty || apiType.isEmpty) {
      showAlertDialog();
      return;
    }
    widget.onConfirmCallback(name, description, schemaType, schemaText, apiType, apiText);
    Get.back();
  }

  @override
  Widget build(BuildContext context) {
    if (_needInit) {
      initData();
      _needInit = false;
    }
    return Center(
      child: Container(
        width: 538,
        height: 438,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.all(Radius.circular(6)),
        ),
        child: Column(
          children: [
            buildTitleContainer(),
            Expanded(
              child: SingleChildScrollView(
                  child: Container(
                margin: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    buildToolDesColumn(),
                    buildSchemaInputColumn(),
                    buildAPIInputColumn(),
                    const SizedBox(height: 10),
                    buildBottomButton()
                  ],
                ),
              )),
            )
          ],
        ),
      ),
    );
  }

  Row buildBottomButton() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(2),
                  side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0),
                ))),
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
            },
            child: const Text('确定', style: TextStyle(color: Colors.white, fontSize: 14)))
      ],
    );
  }

  Column buildAPIInputColumn() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        /*const Row(
        children: [
          Text("API Key", style: TextStyle(fontSize: 14, color: Colors.black)),
          SizedBox(width: 10),
          Icon(Icons.privacy_tip_sharp, size: 14)
        ],
      ),*/
        const Text("API Key", style: TextStyle(fontSize: 14, color: Colors.black)),
        Container(margin: const EdgeInsets.fromLTRB(0, 18, 0, 12), child: const Divider(height: 0.1)),
        Container(
            margin: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(children: [
              Row(
                children: [
                  const Text("类型", style: TextStyle(fontSize: 14, color: Colors.black)),
                  Container(
                    margin: const EdgeInsets.only(left: 10),
                    child: const Text("*", style: TextStyle(fontSize: 14, color: Colors.red)),
                  ),
                ],
              ),
              Container(
                  height: 36,
                  margin: const EdgeInsets.symmetric(vertical: 8),
                  decoration: BoxDecoration(
                    border: Border.all(color: itemBorderColor),
                    borderRadius: const BorderRadius.all(Radius.circular(4)),
                  ),
                  child: Center(
                      child: DropdownButtonHideUnderline(
                    child: DropdownButton2(
                      isExpanded: true,
                      items: apiTypeList
                          .map<DropdownMenuItem<String>>((String item) => DropdownMenuItem<String>(
                                value: item,
                                child: Text(item, style: const TextStyle(fontSize: 14)),
                              ))
                          .toList(),
                      value: _selectAPIType,
                      hint: const Text("这里显示key类型", style: TextStyle(fontSize: 14)),
                      onChanged: (value) {
                        setState(() {
                          _selectAPIType = value;
                        });
                      },
                      dropdownStyleData:
                          const DropdownStyleData(offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
                    ),
                  ))),
              const SizedBox(height: 10),
              buildInputWidget("Key值", true, 75, null, "请输入API Key", _apiTextController),
            ]))
      ],
    );
  }

  Column buildSchemaInputColumn() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        /*const Row(
          children: [
            Text("Schema", style: TextStyle(fontSize: 14, color: Colors.black)),
            SizedBox(width: 10),
            Icon(Icons.privacy_tip_sharp, size: 14)
          ],
        ),*/
        const Text("Schema", style: TextStyle(fontSize: 14, color: Colors.black)),
        Container(margin: const EdgeInsets.fromLTRB(0, 18, 0, 12), child: const Divider(height: 0.1)),
        Container(
            margin: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(children: [
              Row(children: [
                const Text("类型", style: TextStyle(fontSize: 14, color: Colors.black)),
                Container(
                  margin: const EdgeInsets.only(left: 10),
                  child: const Text("*", style: TextStyle(fontSize: 14, color: Colors.red)),
                ),
              ]),
              Container(
                  height: 36,
                  margin: const EdgeInsets.symmetric(vertical: 8),
                  decoration: BoxDecoration(
                    border: Border.all(color: itemBorderColor),
                    borderRadius: const BorderRadius.all(Radius.circular(4)),
                  ),
                  child: Center(
                      child: DropdownButtonHideUnderline(
                    child: DropdownButton2(
                      isExpanded: true,
                      items: schemaTypeList
                          .map<DropdownMenuItem<String>>((String item) => DropdownMenuItem<String>(
                                value: item,
                                child: Text(item, style: const TextStyle(fontSize: 14)),
                              ))
                          .toList(),
                      value: _selectSchemaType,
                      hint: const Text("这里显示协议类型", style: TextStyle(fontSize: 14)),
                      onChanged: (value) {
                        setState(() {
                          _selectSchemaType = value;
                        });
                      },
                      dropdownStyleData:
                          const DropdownStyleData(offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
                    ),
                  ))),
              const SizedBox(height: 10),
              buildInputWidget("文稿", true, 175, null, "请输入schema文稿", _schemaTextController),
            ]))
      ],
    );
  }

  Column buildInputWidget(String title, bool isRequired, double itemHeight, int? maxLines, String hint, TextEditingController controller) {
    var singleLine = maxLines == 1;
    var textField = TextField(
        controller: controller,
        cursorColor: Colors.blue,
        maxLines: maxLines,
        decoration: InputDecoration(
            hintText: hint, border: InputBorder.none, isDense: singleLine, contentPadding: const EdgeInsets.symmetric(horizontal: 8)),
        style: const TextStyle(fontSize: 14));
    return Column(children: [
      Row(children: [
        Text(title, style: const TextStyle(fontSize: 14, color: Colors.black)),
        if (isRequired)
          Container(
            margin: const EdgeInsets.only(left: 10),
            child: const Text("*", style: TextStyle(fontSize: 14, color: Colors.red)),
          )
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
      buildInputWidget("工具名称", true, 40, 1, "请输入工具名称", _nameController),
      buildInputWidget("描述", false, 82, null, "用简单几句话将工具介绍给用户", _desController),
    ]);
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        Text(widget.isEdit ? "编辑工具" : "新建工具"),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () {
            Get.back();
          },
        )
      ]),
    );
  }

  void showAlertDialog() {
    Get.dialog(Center(
        child: Container(
      width: 200,
      height: 100,
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.all(Radius.circular(6)),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Text("必填项不能为空"),
          const SizedBox(height: 10),
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                  backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                    RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(2),
                    ),
                  )),
              onPressed: () {
                Get.back();
              },
              child: const Text("确定", style: TextStyle(color: Colors.white, fontSize: 14)))
        ],
      ),
    )));
  }
}
