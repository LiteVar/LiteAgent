import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/utils/tool/tool_converter.dart';
import 'package:lite_agent_client/utils/tool/tool_validator.dart';
import 'package:lite_agent_client/widgets/dialog/tool_edit/controller.dart';
import 'package:lite_agent_client/widgets/radio_group_widget.dart';

/// 工具编辑对话框的 UI 组件集合
class ToolEditUIComponents {
  static const itemBorderColor = Color(0xFFd9d9d9);

  static Widget buildTitleContainer({required bool isEdit}) {
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

  static Widget buildToolNameAndDesColumn(EditToolDialogController controller) {
    return Column(children: [
      _buildInputWidget(
          title: "工具名称", isRequired: true, itemHeight: 40, maxLines: 1, hint: "请输入工具名称", controller: controller.nameController),
      _buildInputWidget(title: "描述", itemHeight: 82, hint: "用简单几句话将工具介绍给用户", controller: controller.desController),
    ]);
  }

  static Widget buildSchemaInputColumn(EditToolDialogController controller) {
    var items = ToolValidator.supportedLocalTypes
        .map<DropdownMenuItem<String>>((item) => DropdownMenuItem<String>(
              value: item,
              child: Text(ToolConverter.getSchemaTypeOptionString(item), style: const TextStyle(fontSize: 14)),
            ))
        .toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("Schema", style: TextStyle(fontSize: 14, color: Colors.black)),
        Container(margin: const EdgeInsets.fromLTRB(0, 18, 0, 12), child: const Divider(height: 0.1)),
        Container(
            margin: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              _buildInputWidgetTitle("类型", true),
              Obx(() => _buildDropdownContainer(
                    items: items,
                    value: controller.selectSchemaType.value,
                    hint: "这里显示协议类型",
                    onChanged: (value) => controller.selectSchemaType.value = value,
                  )),
              const SizedBox(height: 10),
              Obx(() {
                if (controller.selectSchemaType.value == ToolValidator.OPTION_OPENTOOL_SERVER) {
                  return _buildOpenToolInputServerColumn(controller);
                } else {
                  return _buildInputWidget(
                      title: "文稿", isRequired: true, itemHeight: 175, hint: "请输入schema文稿", controller: controller.schemaTextController);
                }
              })
            ]))
      ],
    );
  }

  static Widget buildAPIInputColumn(EditToolDialogController controller) {
    var items = EditToolDialogController.apiTypeList
        .map<DropdownMenuItem<String>>((item) => DropdownMenuItem<String>(
              value: item,
              child: Text(item, style: const TextStyle(fontSize: 14)),
            ))
        .toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("API Key", style: TextStyle(fontSize: 14, color: Colors.black)),
        Container(margin: const EdgeInsets.fromLTRB(0, 18, 0, 12), child: const Divider(height: 0.1)),
        Container(
            margin: const EdgeInsets.symmetric(horizontal: 12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              _buildInputWidgetTitle("认证类型", false),
              Obx(() => _buildDropdownContainer(
                    items: items,
                    value: controller.selectAPIType.value,
                    hint: "这里显示key类型",
                    onChanged: (value) => controller.selectAPIType.value = value,
                  )),
              const SizedBox(height: 10),
              _buildInputWidget(title: "Key值", itemHeight: 75, hint: "请输入API Key", controller: controller.apiTextController),
            ]))
      ],
    );
  }

  static Widget buildOtherSettingColumn(EditToolDialogController controller) {
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
                child: Obx(() => RadioGroupWidget.buildRadioGroup<bool>(
                      options: const [
                        RadioOption<bool>(value: true, text: "是"),
                        RadioOption<bool>(value: false, text: "否"),
                      ],
                      groupValue: controller.supportAutoMultiAgents.value,
                      onChanged: (value) => controller.supportAutoMultiAgents.value = value!,
                    )),
              ),
            ]))
      ],
    );
  }

  static Widget buildBottomButton({
    required bool isEdit,
    required VoidCallback onRemove,
    required VoidCallback onExport,
    required VoidCallback onCancel,
    required VoidCallback onConfirm,
  }) {
    return Row(
      children: [
        if (isEdit) ...[
          _buildButton(text: '删除', onPressed: onRemove, textColor: const Color(0xffD43030)),
          const SizedBox(width: 16),
          _buildButton(text: '导出', onPressed: onExport, textColor: const Color(0xA6000000)),
        ],
        const Spacer(),
        _buildButton(text: '取消', onPressed: onCancel, textColor: const Color(0xA6000000)),
        const SizedBox(width: 16),
        _buildButton(text: '确定', onPressed: onConfirm, textColor: Colors.white, backgroundColor: const Color(0xFF2a82f5), hasBorder: false),
      ],
    );
  }

  static Widget _buildOpenToolInputServerColumn(EditToolDialogController controller) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildInputWidgetTitle("数据来源", true),
        Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Container(
            margin: const EdgeInsets.symmetric(vertical: 12),
            child: Obx(() => RadioGroupWidget.buildRadioGroup<String>(
                  options: const [RadioOption<String>(value: "server", text: "从服务器获取"), RadioOption<String>(value: "input", text: "手动输入")],
                  groupValue: controller.dataSource.value,
                  onChanged: (value) => controller.onDataSourceChanged(value!),
                )),
          ),
          Obx(() => _buildSchemaFetchUI(controller, controller.dataSource.value == "server"))
        ]),
        const SizedBox(height: 20),
      ],
    );
  }

  static Widget _buildSchemaFetchUI(EditToolDialogController controller, bool isServerFetch) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(children: [
          Container(
            height: 40,
            margin: const EdgeInsets.symmetric(vertical: 8),
            padding: const EdgeInsets.symmetric(horizontal: 10),
            decoration: const BoxDecoration(
                color: Color(0xFFf5f5f5),
                border: Border.fromBorderSide(BorderSide(color: itemBorderColor)),
                borderRadius: BorderRadius.all(Radius.circular(4))),
            child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10),
                child: const Center(child: Text("API Key", style: TextStyle(fontSize: 14, color: Color(0xFF999999))))),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: _buildInputWidgetNoTitle(itemHeight: 40, maxLines: 1, hint: "请输入API Key", controller: controller.serverApiKeyController),
          )
        ]),
        buildServerFetchButtonRow(controller, isServerFetch),
        const SizedBox(height: 10),
        _buildInputWidgetNoTitle(
          itemHeight: 175,
          hint: isServerFetch ? "获取服务器地址后，这里将显示获取内容" : "请输入文稿内容（必填项）",
          controller: controller.openToolSchemaController,
          focusNode: controller.openToolSchemaFocusNode,
          readOnly: isServerFetch,
        )
      ],
    );
  }

  static Row buildServerFetchButtonRow(EditToolDialogController controller, bool isServerFetch) {
    var loadingWidget = const SizedBox(
        width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, valueColor: AlwaysStoppedAnimation<Color>(Colors.white)));
    var buttonWidget = const Text("获取", style: TextStyle(fontSize: 14, color: Colors.white));
    return Row(children: [
      Expanded(
        child: _buildInputWidgetNoTitle(itemHeight: 40, maxLines: 1, hint: "请输入服务器地址（必填项）", controller: controller.serverUrlController),
      ),
      if (isServerFetch) ...[
        const SizedBox(width: 10),
        InkWell(
          onTap: controller.isServerFetching.value ? null : () => controller.fetchFromServer(),
          borderRadius: BorderRadius.circular(4),
          child: Container(
              height: 40,
              width: 80,
              padding: const EdgeInsets.symmetric(horizontal: 20),
              decoration: BoxDecoration(color: const Color(0xFF2a82f5), borderRadius: BorderRadius.circular(4)),
              child: Center(child: controller.isServerFetching.value ? loadingWidget : buttonWidget)),
        )
      ]
    ]);
  }

  // ================================
  // 通用 UI 构建方法
  // ================================
  /// 通用按钮构建方法
  static Widget _buildButton(
      {required String text, required VoidCallback onPressed, required Color textColor, Color? backgroundColor, bool hasBorder = true}) {
    return TextButton(
      style: ButtonStyle(
        padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
        overlayColor: WidgetStateProperty.all(Colors.transparent),
        backgroundColor: backgroundColor != null ? WidgetStateProperty.all(backgroundColor) : null,
        shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(2),
          side: hasBorder ? const BorderSide(color: Color(0xFFd9d9d9), width: 1.0) : BorderSide.none,
        )),
      ),
      onPressed: onPressed,
      child: Text(text, style: TextStyle(color: textColor, fontSize: 14)),
    );
  }

  /// 通用输入组件
  static Widget _buildInputWidget({
    required String title,
    bool isRequired = false,
    required double itemHeight,
    int? maxLines,
    String? hint,
    required TextEditingController controller,
  }) {
    return Column(children: [
      _buildInputWidgetTitle(title, isRequired),
      _buildInputWidgetNoTitle(itemHeight: itemHeight, maxLines: maxLines, hint: hint, verticalMargin: 15, controller: controller)
    ]);
  }

  static Widget _buildInputWidgetNoTitle({
    required double itemHeight,
    int? maxLines,
    String? hint,
    double? verticalMargin = 0,
    required TextEditingController controller,
    FocusNode? focusNode,
    bool readOnly = false,
  }) {
    var singleLine = maxLines == 1;
    var textField = TextField(
        controller: controller,
        focusNode: focusNode,
        cursorColor: Colors.blue,
        maxLines: maxLines,
        readOnly: readOnly,
        decoration: InputDecoration(
            hintStyle: const TextStyle(color: Color(0x40000000)),
            hintText: hint,
            border: InputBorder.none,
            isDense: singleLine,
            contentPadding: const EdgeInsets.symmetric(horizontal: 8)),
        style: const TextStyle(fontSize: 14, color: Color(0xff333333)));
    return Container(
        height: itemHeight,
        margin: EdgeInsets.symmetric(vertical: verticalMargin ?? 0.0),
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
        decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(2)),
        child: singleLine ? Center(child: textField) : textField);
  }

  /// 输入组件标题构建方法
  static Widget _buildInputWidgetTitle(String title, bool isRequired) {
    return Row(children: [
      Text(title, style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
      if (isRequired)
        Container(margin: const EdgeInsets.only(left: 10), child: const Text("*", style: TextStyle(fontSize: 14, color: Colors.red)))
    ]);
  }

  /// 通用下拉选择框构建方法
  static Widget _buildDropdownContainer({
    required List<DropdownMenuItem<String>> items,
    required String? value,
    required String hint,
    required ValueChanged<String?> onChanged,
  }) {
    return Container(
        height: 36,
        margin: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: const BorderRadius.all(Radius.circular(4))),
        child: Center(
          child: DropdownButtonHideUnderline(
            child: DropdownButton2(
              isExpanded: true,
              items: items,
              value: value,
              hint: Text(hint, style: const TextStyle(fontSize: 14, color: Color(0x40000000))),
              onChanged: onChanged,
              dropdownStyleData:
                  const DropdownStyleData(offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
            ),
          ),
        ));
  }
}
