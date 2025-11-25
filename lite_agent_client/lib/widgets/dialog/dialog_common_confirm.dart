import 'package:flutter/material.dart';
import 'package:get/get.dart';

class CommonConfirmDialog extends StatelessWidget {
  final String title;
  final String content;
  final String confirmString;
  final Future<void> Function()? onConfirmCallback;

  const CommonConfirmDialog({
    required this.title,
    required this.content,
    required this.confirmString,
    required this.onConfirmCallback,
  });

  @override
  Widget build(BuildContext context) {
    final screenSize = Get.size;
    final maxWidth = screenSize.width * 0.6; // 屏幕宽度的60%
    final maxHeight = screenSize.height * 0.8; // 屏幕高度的80%
    final minWidth = 360.0; // 最小宽度
    final minHeight = 180.0; // 最小高度
    final defaultWidth = 538.0; // 默认宽度

    return Center(
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: maxWidth < defaultWidth ? maxWidth : defaultWidth,
          maxHeight: maxHeight,
          minWidth: minWidth,
          minHeight: minHeight,
        ),
        child: Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.all(Radius.circular(6)),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildTitleContainer(),
              Flexible(
                child: SingleChildScrollView(
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(24, 20, 24, 20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(content, style: const TextStyle(fontSize: 14, height: 1.5, color: Color(0xFF333333))),
                        const SizedBox(height: 24),
                        buildBottomButton(),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget buildBottomButton() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        if (onConfirmCallback != null)
          TextButton(
            style: ButtonStyle(
              minimumSize: MaterialStateProperty.all(const Size(100, 36)),
              padding: MaterialStateProperty.all(const EdgeInsets.symmetric(horizontal: 24, vertical: 8)),
              shape: MaterialStateProperty.all<RoundedRectangleBorder>(
                RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(4), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)),
              ),
            ),
            onPressed: () => Get.back(),
            child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14)),
          ),
        const SizedBox(width: 16),
        TextButton(
          style: ButtonStyle(
            minimumSize: MaterialStateProperty.all(const Size(100, 36)),
            padding: MaterialStateProperty.all(const EdgeInsets.symmetric(horizontal: 24, vertical: 8)),
            backgroundColor: MaterialStateProperty.all(const Color(0xFF2a82f5)),
            shape: MaterialStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(4))),
          ),
          onPressed: () async {
            await onConfirmCallback?.call();
            Get.back();
          },
          child: Text(confirmString.isNotEmpty ? confirmString : '确定', style: const TextStyle(color: Colors.white, fontSize: 14)),
        ),
      ],
    );
  }

  Container _buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        Text(title, style: const TextStyle(fontSize: 14, color: Colors.black)),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () => Get.back(),
        )
      ]),
    );
  }
}
