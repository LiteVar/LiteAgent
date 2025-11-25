import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_common_confirm.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_export_confirm.dart';
import 'package:lite_agent_client/widgets/dialog/tool_edit/components.dart';
import 'package:lite_agent_client/widgets/dialog/tool_edit/controller.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';

class EditToolDialog extends StatelessWidget {
  final controller = Get.put(EditToolDialogController());
  final ToolModel? tool;
  final bool isEdit;
  final void Function(ToolFormData? toolData, {bool isDelete}) onConfirmCallback;

  EditToolDialog({super.key, required this.tool, required this.isEdit, required this.onConfirmCallback}) {
    var tool = this.tool;
    controller.initData(tool);
  }

  Future<void> _confirm() async {
    ToolFormData? toolData = await controller.getFormDataAndValidate();
    if (toolData != null) {
      onConfirmCallback(toolData);
      Get.back();
    }
  }

  void showRemoveDialog() {
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
          title: "删除确认",
          content: "即将删除工具的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () async {
            onConfirmCallback(null, isDelete: true);
            Get.back();
          },
        ));
  }

  void showExportConfirmDialog() {
    Get.dialog(
        barrierDismissible: false,
        ExportConfirmDialog(
          exportType: "工具",
          onConfirmCallback: (bool exportPlaintext) {
            controller.exportToolData(exportPlaintext);
          },
        ));
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 588,
        height: 538,
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
        child: Column(children: [
          ToolEditUIComponents.buildTitleContainer(isEdit: isEdit),
          Expanded(
            child: SingleChildScrollView(
              child: Container(
                margin: const EdgeInsets.all(20),
                child: Column(children: [
                  ToolEditUIComponents.buildToolNameAndDesColumn(controller),
                  ToolEditUIComponents.buildSchemaInputColumn(controller),
                  Obx(() => Offstage(
                        offstage: !(controller.selectSchemaType.value == Protocol.OPENAPI ||
                            controller.selectSchemaType.value == Protocol.JSONRPCHTTP),
                        child: ToolEditUIComponents.buildAPIInputColumn(controller),
                      )),
                  ToolEditUIComponents.buildOtherSettingColumn(controller),
                  const SizedBox(height: 10),
                  ToolEditUIComponents.buildBottomButton(
                    isEdit: isEdit,
                    onRemove: showRemoveDialog,
                    onExport: showExportConfirmDialog,
                    onCancel: () => Get.back(),
                    onConfirm: _confirm,
                  ),
                ]),
              ),
            ),
          ),
        ]),
      ),
    );
  }
}
