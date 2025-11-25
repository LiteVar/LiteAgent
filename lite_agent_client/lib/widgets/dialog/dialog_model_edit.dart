import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/extension/model_extension.dart';
import 'package:lite_agent_client/utils/model/model_converter.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_export_confirm.dart';

import 'dialog_common_confirm.dart';

class EditModelDialog extends StatelessWidget {
  final ModelData? model;
  final bool isEdit;
  bool? isKnowledgeModel;
  final void Function(ModelFormData? modelData, {bool isDelete}) onConfirmCallback;

  EditModelDialog({super.key, required this.model, required this.isEdit, this.isKnowledgeModel = false, required this.onConfirmCallback}) {
    logic.initData(model);
  }

  final dialogTitleColor = const Color(0xFFf5f5f5);
  final buttonColor = const Color(0xFF2a82f5);
  final itemBorderColor = const Color(0xFFd9d9d9);
  final logic = Get.put(EditModelDialogController());

  Future<void> _confirm() async {
    String type = logic.modelType.value;
    String name = logic.nameController.text.trimmed();
    String alias = logic.aliasController.text.trimmed();
    String baseUrl = logic.urlController.text.trimmed();
    String apiKey = logic.apiController.text.trimmed();
    String maxToken = logic.maxTokenController.text.trimmed();
    bool isLLM = type == ModelValidator.LLM;
    bool supportMultiAgent = isLLM ? logic.supportMultiAgent.value : false;
    bool supportToolCalling = isLLM ? logic.supportToolCalling.value : false;
    bool supportDeepThinking = isLLM ? logic.supportDeepThinking.value : false;

    if (name.trim().isEmpty) {
      AlarmUtil.showAlertDialog("模型名称不能为空");
      return;
    }
    if (alias.isEmpty) {
      AlarmUtil.showAlertDialog("连接别名不能为空");
      return;
    }
    if (baseUrl.trim().isEmpty) {
      AlarmUtil.showAlertDialog("BaseUrl不能为空");
      return;
    }
    Uri? uri = Uri.tryParse(baseUrl);
    if (uri == null || (!uri.hasScheme || (uri.scheme != 'http' && uri.scheme != 'https')) || uri.host.isEmpty) {
      if (!(isKnowledgeModel ?? false)) {
        AlarmUtil.showAlertDialog("BaseUrl格式错误");
        return;
      }
    }
    if (apiKey.trim().isEmpty) {
      AlarmUtil.showAlertDialog("ApiKey不能为空");
      return;
    }
    if (maxToken.trim().isEmpty) {
      //AlarmUtil.showAlertDialog("maxToken不能为空");
      //return;
      maxToken = "4096";
    } else if (int.parse(maxToken) < 0) {
      AlarmUtil.showAlertDialog("maxToken最小值为1");
      return;
    }

    if (!await ModelValidator.isAliasUniqueAsync(alias, excludeId: model?.id)) {
      AlarmUtil.showAlertDialog("该别名已存在，请重新输入");
      return;
    }

    onConfirmCallback((
      name: name,
      alias: alias,
      baseUrl: baseUrl,
      apiKey: apiKey,
      maxToken: maxToken,
      modelType: type,
      supportMultiAgent: supportMultiAgent,
      supportToolCalling: supportToolCalling,
      supportDeepThinking: supportDeepThinking,
    ));
    Get.back();
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 588,
        constraints: BoxConstraints(minHeight: 238, maxHeight: MediaQuery.of(context).size.height * 0.8),
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            buildTitleContainer(),
            Flexible(
              child: SingleChildScrollView(
                child: Container(
                  margin: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (!(isKnowledgeModel ?? false)) ...[
                        Row(
                          children: [
                            Container(
                              margin: const EdgeInsets.fromLTRB(5, 0, 10, 0),
                              child: const Text("模型类型", style: TextStyle(fontSize: 14, color: Colors.black)),
                            ),
                            const Text("*", style: TextStyle(fontSize: 14, color: Colors.red))
                          ],
                        ),
                        Obx(() => Container(
                            padding: const EdgeInsets.symmetric(vertical: 2),
                            child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                              _buildRadioOption(ModelValidator.LLM),
                              _buildRadioOption(ModelValidator.TTS),
                              _buildRadioOption(ModelValidator.ASR),
                            ])))
                      ],
                      const SizedBox(height: 5),
                      buildInputColumn(
                          title: "模型名称",
                          hint: "请输入官方的模型名称",
                          underLineTips: "这是模型的官方名称，用于系统识别其能力和计费标准。",
                          controller: logic.nameController,
                          isRequired: true,
                          textLimit: 100),
                      buildInputColumn(
                          title: "连接别名",
                          hint: "请输入别名，如我的备用模型，xx专用模型之类",
                          underLineTips: "同个模型的名称通过不同API Key可以有多个，所以设置一个易于区分的名称。",
                          controller: logic.aliasController,
                          isRequired: true,
                          textLimit: 100),
                      buildInputColumn(title: "BaseURL", hint: "请输入URL", controller: logic.urlController, isRequired: true),
                      buildInputColumn(title: "Key", hint: "请输入key值", controller: logic.apiController, isRequired: true),
                      buildInputColumn(
                          title: "Max Token", hint: "请输入Max Token，最小值为1", controller: logic.maxTokenController, isNumberOnly: true),
                      Obx(() => Offstage(
                            offstage: logic.modelType.value != ModelValidator.LLM,
                            child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                              Expanded(child: buildFunctionSwitchContainer("支持Auto Multi Agent使用", logic.supportMultiAgent)),
                              Expanded(child: buildFunctionSwitchContainer("支持工具调用", logic.supportToolCalling)),
                              Expanded(child: buildFunctionSwitchContainer("支持深度思考", logic.supportDeepThinking)),
                            ]),
                          )),
                      const SizedBox(height: 10),
                      buildBottomButton()
                    ],
                  ),
                ),
              ),
            )
          ],
        ),
      ),
    );
  }

  Container buildFunctionSwitchContainer(String title, RxBool switchValue) {
    return Container(
        margin: const EdgeInsets.symmetric(vertical: 10),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: const TextStyle(fontSize: 14, color: Colors.black)),
          const SizedBox(height: 5),
          Obx(() => SwitchTheme(
                data: SwitchThemeData(
                  thumbColor: MaterialStateProperty.all(Colors.white),
                  trackColor: MaterialStateProperty.resolveWith(
                      (states) => Color(states.contains(MaterialState.selected) ? 0xFF1890FF : 0xFFD9D9D9)),
                  trackOutlineColor: MaterialStateProperty.all(Colors.transparent),
                  splashRadius: 0,
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
                child: Transform.scale(
                    scale: 0.8,
                    child: Switch(
                        value: switchValue.value,
                        onChanged: (value) => switchValue.value = value,
                        overlayColor: MaterialStateProperty.all(Colors.transparent))),
              )),
        ]));
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      decoration: BoxDecoration(
          color: dialogTitleColor, borderRadius: const BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6))),
      child: Row(children: [
        Text(isEdit ? "编辑模型" : "新建模型"),
        const Spacer(),
        IconButton(icon: const Icon(Icons.close, size: 16, color: Colors.black), onPressed: () => Get.back())
      ]),
    );
  }

  Row buildBottomButton() {
    return Row(
      children: [
        if (isEdit && !(isKnowledgeModel ?? false)) ...[
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                  overlayColor: WidgetStateProperty.all(Colors.transparent),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)))),
              onPressed: () => showRemoveDialog(),
              child: const Text('删除', style: TextStyle(color: Color(0xffD43030), fontSize: 14))),
          const SizedBox(width: 16),
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                  overlayColor: WidgetStateProperty.all(Colors.transparent),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)))),
              onPressed: () => showExportConfirmDialog(logic),
              child: const Text('导出', style: TextStyle(color: Color(0xA6000000), fontSize: 14)))
        ],
        const Spacer(),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                overlayColor: WidgetStateProperty.all(Colors.transparent),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)))),
            onPressed: () => Get.back(),
            child: const Text('取消', style: TextStyle(color: Color(0xA6000000), fontSize: 14))),
        const SizedBox(width: 16),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                overlayColor: WidgetStateProperty.all(Colors.transparent),
                backgroundColor: WidgetStateProperty.all(buttonColor),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(2)))),
            onPressed: () => _confirm(),
            child: const Text('确定', style: TextStyle(color: Colors.white, fontSize: 14)))
      ],
    );
  }

  Column buildInputColumn({
    required String title,
    required String hint,
    String underLineTips = "",
    bool isRequired = false,
    double height = 40.0,
    int? textLimit,
    bool isNumberOnly = false,
    required TextEditingController controller,
  }) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Row(children: [
        Container(
          margin: const EdgeInsets.fromLTRB(5, 0, 10, 0),
          child: Text(title, style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
        ),
        if (isRequired) const Text("*", style: TextStyle(fontSize: 14, color: Colors.red))
      ]),
      Container(
          height: height,
          margin: const EdgeInsets.symmetric(vertical: 10),
          padding: const EdgeInsets.symmetric(horizontal: 8),
          decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(2)),
          child: Center(
              child: TextField(
                  controller: controller,
                  maxLines: 1,
                  maxLength: textLimit,
                  inputFormatters: isNumberOnly ? [FilteringTextInputFormatter.allow(RegExp(r'[0-9]'))] : null,
                  cursorColor: const Color(0xff2A82E4),
                  decoration: InputDecoration(
                    hintText: hint,
                    hintStyle: const TextStyle(fontSize: 14, color: Color(0xff999999)),
                    border: InputBorder.none,
                    isDense: true,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 8),
                    counterText: "",
                  ),
                  style: const TextStyle(fontSize: 14, color: Color(0xff333333))))),
      if (underLineTips.isNotEmpty)
        Container(
            margin: const EdgeInsets.only(bottom: 10),
            child: Text(underLineTips, style: const TextStyle(fontSize: 14, color: Color(0xff999999)))),
    ]);
  }

  Widget _buildRadioOption(String option) {
    return Expanded(
      child: RadioListTile<String>(
        title: Text(option.toUpperCase(), style: const TextStyle(fontSize: 14)),
        value: option,
        groupValue: ModelConverter.normalizeModelType(logic.modelType.value),
        visualDensity: const VisualDensity(horizontal: VisualDensity.minimumDensity, vertical: VisualDensity.minimumDensity),
        materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
        controlAffinity: ListTileControlAffinity.leading,
        dense: true,
        activeColor: Colors.blue,
        contentPadding: EdgeInsets.zero,
        toggleable: true,
        onChanged: (value) {
          if (value != null) {
            logic.modelType.value = value;
          }
        },
      ),
    );
  }

  void showRemoveDialog() {
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
          title: "删除确认",
          content: "即将删除模型的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () async {
            onConfirmCallback(null, isDelete: true);
            Get.back();
          },
        ));
  }
}

void showExportConfirmDialog(EditModelDialogController logic) {
  Get.dialog(
      barrierDismissible: false,
      ExportConfirmDialog(
        exportType: "模型",
        onConfirmCallback: (exportPlaintext) => logic.exportModelData(exportPlaintext),
      ));
}

typedef ModelFormData = ({
  String name,
  String baseUrl,
  String apiKey,
  String maxToken,
  String modelType,
  String alias,
  bool supportMultiAgent,
  bool supportToolCalling,
  bool supportDeepThinking,
});

class EditModelDialogController extends GetxController {
  final TextEditingController nameController = TextEditingController();
  final TextEditingController aliasController = TextEditingController();
  final TextEditingController urlController = TextEditingController();
  final TextEditingController apiController = TextEditingController();
  final TextEditingController maxTokenController = TextEditingController();

  RxString modelType = ModelValidator.LLM.obs;
  RxBool supportMultiAgent = false.obs;
  RxBool supportToolCalling = true.obs;
  RxBool supportDeepThinking = false.obs;

  void initData(ModelData? params) {
    modelType.value = params?.type ?? ModelValidator.LLM;
    nameController.text = params?.name ?? "";
    aliasController.text = params?.alias ?? "";
    urlController.text = params?.url ?? "";
    apiController.text = params?.key ?? "";
    maxTokenController.text = params?.maxToken ?? "4096";
    supportMultiAgent.value = params?.supportMultiAgent ?? false;
    supportToolCalling.value = params?.supportToolCalling ?? true;
    supportDeepThinking.value = params?.supportDeepThinking ?? false;
  }

  Future<void> exportModelData(bool exportPlaintext) async {
    String type = modelType.value;
    String name = nameController.text.trimmed();
    String alias = aliasController.text.trimmed();
    String baseUrl = urlController.text.trimmed();
    String apiKey = apiController.text.trimmed();
    int maxToken = int.tryParse(maxTokenController.text.trimmed()) ?? 4096;
    bool isLLM = type == ModelValidator.LLM;
    bool autoAgent = isLLM ? supportMultiAgent.value : false;
    bool toolInvoke = isLLM ? supportToolCalling.value : false;
    bool deepThink = isLLM ? supportDeepThinking.value : false;

    final modelDTO = ModelDTO("", alias, name, baseUrl, apiKey, maxToken, type, autoAgent, toolInvoke, deepThink, "", 0);

    String? savePath = await modelDTO.exportJson(exportPlaintext);
    bool isSuccess = savePath != null;
    AlarmUtil.showAlertToast(isSuccess ? "模型数据导出成功！" : "导出失败");
  }

  @override
  void onClose() {
    nameController.dispose();
    aliasController.dispose();
    urlController.dispose();
    apiController.dispose();
    maxTokenController.dispose();
    super.onClose();
  }
}
