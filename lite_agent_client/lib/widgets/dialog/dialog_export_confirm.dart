import 'package:flutter/material.dart';
import 'package:get/get.dart';

class ExportConfirmDialog extends StatelessWidget {
  final void Function(bool exportPlaintext) onConfirmCallback;
  final String exportType;

  const ExportConfirmDialog({super.key, required this.onConfirmCallback, required this.exportType});

  final dialogTitleColor = const Color(0xFFf5f5f5);
  final buttonColor = const Color(0xFF2a82f5);
  final textColor = const Color(0xFF333333);
  final warningTextColor = const Color(0xFF666666);

  @override
  Widget build(BuildContext context) {
    final controller = Get.put(ExportConfirmDialogController());

    return Center(
      child: Container(
        width: 480,
        height: 300,
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildTitleContainer(),
            Container(
              padding: const EdgeInsets.fromLTRB(24, 20, 24, 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 第一段说明文字
                  Text(
                    "为保障您的账户安全，$exportType中包含的 API Key 将默认以“暗文” 形式导出。“暗文”导出的 Key 无法在导出的文件中直接被读取，在重新导入平台时需重新设置才可正常使用。",
                    style: const TextStyle(fontSize: 14, height: 1.5, color: Color(0xFF333333)),
                  ),
                  const SizedBox(height: 16),
                  // 复选框选项
                  Obx(() => Row(
                        children: [
                          Checkbox(
                            value: controller.exportPlaintext.value,
                            onChanged: (value) {
                              controller.exportPlaintext.value = value ?? false;
                            },
                            activeColor: buttonColor,
                            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                          InkWell(
                            onTap: () => controller.exportPlaintext.value = !controller.exportPlaintext.value,
                            child: const Text("以明文形式导出 API Key", style: TextStyle(fontSize: 14, color: Color(0xFF333333))),
                          ),
                        ],
                      )),
                  const SizedBox(height: 16),
                  // 警告文字
                  const Text(
                    "请注意：开启后，API Key 的完整内容将直接显示在导出的文件中。这可能带来安全风险，请确保您在安全的环境下存储和传输该文件。",
                    style: TextStyle(fontSize: 13, height: 1.4, color: Color(0xFF666666)),
                  ),
                  const SizedBox(height: 24),
                  // 底部按钮
                  _buildBottomButtons(controller),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Container _buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
      decoration: BoxDecoration(
        color: dialogTitleColor,
        borderRadius: const BorderRadius.only(
          topLeft: Radius.circular(6),
          topRight: Radius.circular(6),
        ),
      ),
      child: Row(
        children: [
          Text("导出$exportType", style: const TextStyle(fontSize: 14, color: Colors.black, fontWeight: FontWeight.w500)),
          const Spacer(),
          IconButton(
            icon: const Icon(Icons.close, size: 16, color: Colors.black),
            onPressed: () => Get.back(),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomButtons(ExportConfirmDialogController controller) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        TextButton(
          style: ButtonStyle(
            minimumSize: MaterialStateProperty.all(const Size(80, 36)),
            padding: MaterialStateProperty.all(const EdgeInsets.symmetric(horizontal: 24, vertical: 8)),
            shape: MaterialStateProperty.all<RoundedRectangleBorder>(
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(4), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)),
            ),
            backgroundColor: MaterialStateProperty.all(Colors.white),
          ),
          onPressed: () => Get.back(),
          child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14)),
        ),
        const SizedBox(width: 16),
        TextButton(
          style: ButtonStyle(
            minimumSize: MaterialStateProperty.all(const Size(80, 36)),
            padding: MaterialStateProperty.all(const EdgeInsets.symmetric(horizontal: 24, vertical: 8)),
            backgroundColor: MaterialStateProperty.all(buttonColor),
            shape: MaterialStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(4))),
          ),
          onPressed: () {
            onConfirmCallback(controller.exportPlaintext.value);
            Get.back();
          },
          child: const Text('确定', style: TextStyle(color: Colors.white, fontSize: 14)),
        ),
      ],
    );
  }
}

class ExportConfirmDialogController extends GetxController {
  RxBool exportPlaintext = false.obs; // 默认不勾选

  @override
  void onInit() {
    super.onInit();
    // 确保默认不导出明文
    exportPlaintext.value = false;
  }
}
