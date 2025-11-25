import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/common_button.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_markdown.dart';

/// 输入提示词配置组件
/// 负责系统提示词的输入和预览
class InputPromptConfig extends StatelessWidget {
  // 控制器和状态
  final TextEditingController textController;
  late final RxDouble inputPromptHeight;
  
  // 高度限制
  final double minInputPromptHHeight = 120.0;
  final double maxInputPromptHHeight = 300.0;
  
  // 回调函数
  final VoidCallback? onDataChanged;

  InputPromptConfig({
    super.key,
    required this.textController,
    this.onDataChanged,
  }) {
    inputPromptHeight = 120.0.obs;
  }

  /// 更新提示词高度
  void updateHeight(double deltaY) {
    inputPromptHeight.value = (inputPromptHeight.value + deltaY).clamp(minInputPromptHHeight, maxInputPromptHHeight);
  }

  /// 设置提示词内容
  void setPrompt(String prompt) {
    textController.text = prompt;
  }

  /// 获取当前提示词内容
  String getPrompt() {
    return textController.text;
  }

  /// 获取提示词内容的getter
  String get prompt => textController.text;

  /// 设置提示词内容的setter
  set prompt(String value) {
    textController.text = value;
  }

  /// 添加文本变化监听
  void addTextListener() {
    textController.addListener(() {
      onDataChanged?.call();
    });
  }

  /// 显示提示词预览对话框
  void showPromptPreviewDialog() {
    Get.dialog(
      barrierDismissible: false,
      MarkDownTextDialog(titleText: "提示词预览", contentText: textController.text),
    );
  }

  /// 清理资源
  void dispose() {
    textController.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const SizedBox(height: 10),
        Row(
          children: [
            const Text("系统提示词", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
            const Spacer(),
            CommonBlueButton(
              iconName: "icon_search.png",
              buttonText: "提示词预览",
              onTap: () => showPromptPreviewDialog(),
            ),
          ],
        ),
        Stack(children: [
          Obx(() => Container(
              height: inputPromptHeight.value,
              margin: const EdgeInsets.symmetric(vertical: 10),
              padding: const EdgeInsets.symmetric(vertical: 12),
              decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(4)),
              child: TextField(
                  maxLines: null,
                  controller: textController,
                  decoration: const InputDecoration(
                      hintText: '请输入系统提示词', border: InputBorder.none, isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 10)),
                  style: const TextStyle(fontSize: 14, color: Color(0xff333333))))),
          Positioned(
            right: 0,
            bottom: 10,
            child: GestureDetector(
                onVerticalDragUpdate: (details) => updateHeight(details.delta.dy),
                child: const Icon(Icons.signal_cellular_4_bar, size: 16, color: Color(0xff999999))),
          )
        ]),
      ],
    );
  }
}
