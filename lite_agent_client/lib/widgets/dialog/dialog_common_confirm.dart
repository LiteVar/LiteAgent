import 'package:flutter/material.dart';
import 'package:get/get.dart';

class CommonConfirmDialog extends StatelessWidget {
  final String title;
  final String content;
  final String confirmString;
  final Future<void> Function() onConfirmCallback;

  const CommonConfirmDialog({
    required this.title,
    required this.content,
    required this.confirmString,
    required this.onConfirmCallback,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
        child: Container(
            width: 538,
            height: 213,
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.all(Radius.circular(6)),
            ),
            child: Column(children: [
              _buildTitleContainer(),
              Expanded(
                  child: Container(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(content, style: const TextStyle(fontSize: 14, color: Colors.black)),
                    const Spacer(),
                    buildBottomButton(),
                  ],
                ),
              ))
            ])));
  }

  Widget buildBottomButton() {
    return Row(children: [
      const Spacer(),
      TextButton(
          style: ButtonStyle(
              padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
              shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(2),
                  side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0),
                ),
              )),
          onPressed: () => Get.back(),
          child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14))),
      const SizedBox(width: 14),
      TextButton(
          style: ButtonStyle(
              padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
              backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
              shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(2)),
              )),
          onPressed: () async {
            await onConfirmCallback();
            Get.back();
          },
          child: Text(
            confirmString.isNotEmpty ? confirmString : '确定',
            style: const TextStyle(color: Colors.white, fontSize: 14),
          )),
    ]);
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
