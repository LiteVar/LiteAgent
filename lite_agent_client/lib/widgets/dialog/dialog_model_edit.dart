import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

import 'dialog_common_confirm.dart';

class EditModelDialog extends StatelessWidget {
  ModelBean? model;

  late ModelEditParams params;
  late bool isEdit;
  void Function(ModelEditParams params) onConfirmCallback;

  EditModelDialog({super.key, required this.model, required this.isEdit, required this.onConfirmCallback}) {
    String nickName = "";
    if (model != null) {
      nickName = model?.nickName ?? "模型${model?.id.lastSixChars ?? ""}";
    } else {
      nickName = model?.nickName ?? "";
    }
    int maxToken = 4096;
    String maxTokenString = model?.maxToken ?? "";
    if (maxTokenString.isNotEmpty) {
      maxToken = int.parse(maxTokenString);
    }
    params = ModelEditParams(
      type: model?.type ?? "LLM",
      name: model?.name ?? "",
      nickName: nickName,
      baseUrl: model?.url ?? "",
      apiKey: model?.key ?? "",
      maxToken: maxToken,
      supportMultiAgent: model?.supportMultiAgent ?? false,
      supportToolCalling: model?.supportToolCalling ?? true,
      supportDeepThinking: model?.supportDeepThinking ?? false,
    );
  }

  final dialogTitleColor = const Color(0xFFf5f5f5);
  final buttonColor = const Color(0xFF2a82f5);
  final itemBorderColor = const Color(0xFFd9d9d9);
  final logic = Get.put(EditModelDialogController());

  Future<void> _confirm() async {
    String type = logic.modelType.value;
    String name = logic.nameController.text.trimmed();
    String nickName = logic.nickNameController.text.trimmed();
    String baseUrl = logic.urlController.text.trimmed();
    String apiKey = logic.apiController.text.trimmed();
    String maxToken = logic.maxTokenController.text.trimmed();
    bool supportMultiAgent = type == "LLM" ? logic.supportMultiAgent.value : false;
    bool supportToolCalling = type == "LLM" ? logic.supportToolCalling.value : false;
    bool supportDeepThinking = type == "LLM" ? logic.supportDeepThinking.value : false;

    if (name.trim().isEmpty) {
      AlarmUtil.showAlertDialog("模型名称不能为空");
      return;
    } else if (nickName.isEmpty) {
      AlarmUtil.showAlertDialog("连接别名不能为空");
      return;
    } else if (baseUrl.trim().isEmpty) {
      AlarmUtil.showAlertDialog("BaseUrl不能为空");
      return;
    } else if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
      AlarmUtil.showAlertDialog("BaseUrl格式错误");
      return;
    } else if (apiKey.trim().isEmpty) {
      AlarmUtil.showAlertDialog("ApiKey不能为空");
      return;
    } else if (maxToken.trim().isEmpty) {
      //AlarmUtil.showAlertDialog("maxToken不能为空");
      //return;
      maxToken = "4096";
    } else if (int.parse(maxToken) < 0) {
      AlarmUtil.showAlertDialog("maxToken最小值为1");
      return;
    }

    if (await _hasDuplicateNickname(nickName, model?.id)) {
      AlarmUtil.showAlertDialog("该别名已存在，请重新输入");
      return;
    }

    onConfirmCallback(ModelEditParams(
      type: type,
      name: name,
      nickName: nickName,
      baseUrl: baseUrl,
      apiKey: apiKey,
      maxToken: int.parse(maxToken),
      supportMultiAgent: supportMultiAgent,
      supportToolCalling: supportToolCalling,
      supportDeepThinking: supportDeepThinking,
    ));
    Get.back();
  }

  Future<bool> _hasDuplicateNickname(String newNickname, String? currentId) async {
    final existingModels = await modelRepository.getModelListFromBox();
    return existingModels.any(
        (model) => model.nickName != null && model.nickName!.isNotEmpty && model.nickName == newNickname && model.id != (currentId ?? ""));
  }

  @override
  Widget build(BuildContext context) {
    logic.initData(params);
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
                      Row(children: [
                        Container(
                          margin: const EdgeInsets.fromLTRB(5, 0, 10, 0),
                          child: const Text("模型类型", style: TextStyle(fontSize: 14, color: Colors.black)),
                        ),
                        const Text("*", style: TextStyle(fontSize: 14, color: Colors.red))
                      ]),
                      Obx(() => Container(
                            padding: const EdgeInsets.symmetric(vertical: 2),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [_buildRadioOption('LLM'), _buildRadioOption('TTS'), _buildRadioOption('ASR')],
                            ),
                          )),
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
                          controller: logic.nickNameController,
                          isRequired: true,
                          textLimit: 100),
                      buildInputColumn(title: "BaseURL", hint: "请输入URL", controller: logic.urlController, isRequired: true),
                      buildInputColumn(title: "Key", hint: "请输入key值", controller: logic.apiController, isRequired: true),
                      buildInputColumn(
                          title: "Max Token", hint: "请输入Max Token，最小值为1", controller: logic.maxTokenController, isNumberOnly: true),
                      Obx(() => Offstage(
                            offstage: logic.modelType.value != "LLM",
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
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () => Get.back(),
        )
      ]),
    );
  }

  Row buildBottomButton() {
    return Row(
      children: [
        if (isEdit)
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)))),
              onPressed: () => showRemoveDialog(),
              child: const Text('删除', style: TextStyle(color: Color(0xA6D43030), fontSize: 14))),
        const Spacer(),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)))),
            onPressed: () => Get.back(),
            child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14))),
        const SizedBox(width: 16),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
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
        title: Text(option, style: const TextStyle(fontSize: 14)),
        value: option,
        groupValue: logic.modelType.value,
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
            onConfirmCallback(ModelEditParams.delete());
            Get.back();
          },
        ));
  }
}

class EditModelDialogController extends GetxController {
  final TextEditingController nameController = TextEditingController();
  final TextEditingController nickNameController = TextEditingController();
  final TextEditingController urlController = TextEditingController();
  final TextEditingController apiController = TextEditingController();
  final TextEditingController maxTokenController = TextEditingController();

  RxString modelType = 'LLM'.obs;
  RxBool supportMultiAgent = false.obs;
  RxBool supportToolCalling = true.obs;
  RxBool supportDeepThinking = false.obs;

  void initData(ModelEditParams params) {
    modelType.value = params.type;
    nameController.text = params.name;
    nickNameController.text = params.nickName;
    urlController.text = params.baseUrl;
    apiController.text = params.apiKey;
    maxTokenController.text = params.maxToken.toString();
    supportMultiAgent.value = params.supportMultiAgent;
  }

  @override
  void onClose() {
    nameController.dispose();
    nickNameController.dispose();
    urlController.dispose();
    apiController.dispose();
    maxTokenController.dispose();
    super.onClose();
  }
}

class ModelEditParams {
  final String type;
  final String name;
  final String nickName;
  final String baseUrl;
  final String apiKey;
  final int maxToken;
  final bool supportMultiAgent;
  final bool supportToolCalling;
  final bool supportDeepThinking;
  final bool isDelete;

  ModelEditParams({
    required this.type,
    required this.name,
    required this.nickName,
    required this.baseUrl,
    required this.apiKey,
    required this.maxToken,
    required this.supportMultiAgent,
    required this.supportToolCalling,
    required this.supportDeepThinking,
    this.isDelete = false,
  });

  factory ModelEditParams.delete() {
    return ModelEditParams(
      isDelete: true,
      type: "",
      name: "",
      nickName: "",
      baseUrl: "",
      apiKey: "",
      maxToken: 0,
      supportMultiAgent: false,
      supportToolCalling: false,
      supportDeepThinking: false,
    );
  }
}
