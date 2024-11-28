import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';

class EditModelDialog extends StatelessWidget {
  ModelBean? model;
  late String name;
  late String baseUrl;
  late String apiKey;
  late bool isEdit;
  void Function(String name, String baseUrl, String apiKey) onConfirmCallback;

  EditModelDialog({super.key, required this.model, required this.isEdit, required this.onConfirmCallback}) {
    name = model?.name ?? "";
    baseUrl = model?.url ?? "";
    apiKey = model?.key ?? "";
  }

  final dialogTitleColor = const Color(0xFFf5f5f5);
  final buttonColor = const Color(0xFF2a82f5);
  final itemBorderColor = const Color(0xFFd9d9d9);
  final logic = Get.put(EditModelDialogController());

  void _confirm() {
    String name = logic.nameController.text;
    String baseUrl = logic.urlController.text;
    String apiKey = logic.apiController.text;
    if (name.trim().isEmpty) {
      AlarmUtil.showAlertDialog("模型名称不能为空");
      return;
    } else if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
      AlarmUtil.showAlertDialog("BaseUrl格式错误");
      return;
    } else if (apiKey.trim().isEmpty) {
      AlarmUtil.showAlertDialog("ApiKey不能为空");
      return;
    }
    onConfirmCallback(name, baseUrl, apiKey);
    Get.back();
  }

  @override
  Widget build(BuildContext context) {
    logic.initData(name, baseUrl, apiKey);
    return Center(
      child: Container(
        width: 538,
        height: 447,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.all(Radius.circular(6)),
        ),
        child: Column(
          children: [
            buildTitleContainer(),
            Expanded(
                child: Container(
              margin: const EdgeInsets.all(16),
              child: Column(
                children: [
                  buildInputColumn("模型名称", "请输入模型名称", true, 40, 20, logic.nameController),
                  buildInputColumn("BaseURL", "请输入URL", false, 55, null, logic.urlController),
                  buildInputColumn("API", "请输入key值", true, 55, null, logic.apiController),
                  const SizedBox(height: 10),
                  buildBottomButton()
                ],
              ),
            ))
          ],
        ),
      ),
    );
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
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)),
                )),
            onPressed: () {
              Get.back();
            },
            child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14))),
        const SizedBox(width: 16),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                backgroundColor: WidgetStateProperty.all(buttonColor),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(borderRadius: BorderRadius.circular(2)),
                )),
            onPressed: () {
              _confirm();
            },
            child: const Text('确定', style: TextStyle(color: Colors.white, fontSize: 14)))
      ],
    );
  }

  Column buildInputColumn(String title, String hint, bool isRequired, double height, int? textLimit, TextEditingController controller) {
    return Column(children: [
      Row(children: [
        Container(
          margin: const EdgeInsets.fromLTRB(5, 0, 10, 0),
          child: Text(title, style: const TextStyle(fontSize: 14, color: Colors.black)),
        ),
        if (isRequired) const Text("*", style: TextStyle(fontSize: 14, color: Colors.red))
      ]),
      Container(
          height: height,
          margin: const EdgeInsets.symmetric(vertical: 15),
          padding: const EdgeInsets.symmetric(horizontal: 8),
          decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(2)),
          child: Center(
              child: TextField(
                  controller: controller,
                  maxLines: 1,
                  maxLength: textLimit,
                  decoration: InputDecoration(
                    hintText: hint,
                    border: InputBorder.none,
                    isDense: true,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 8),
                    counterText: "",
                  ),
                  style: const TextStyle(fontSize: 14))))
    ]);
  }
}

class EditModelDialogController extends GetxController {
  final TextEditingController nameController = TextEditingController();
  final TextEditingController urlController = TextEditingController();
  final TextEditingController apiController = TextEditingController();

  void initData(String name, String baseUrl, String apiKey) {
    nameController.text = name;
    urlController.text = baseUrl;
    apiController.text = apiKey;
  }

  @override
  void onClose() {
    nameController.dispose();
    urlController.dispose();
    apiController.dispose();
    super.onClose();
  }
}
