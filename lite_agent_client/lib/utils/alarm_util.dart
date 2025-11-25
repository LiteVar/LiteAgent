import 'package:flutter/material.dart';
import 'package:flutter_styled_toast/flutter_styled_toast.dart';
import 'package:get/get.dart';

class AlarmUtil {
  static void showAlertToast(String message) {
    showToast(message,
        animation: StyledToastAnimation.fade,
        reverseAnimation: StyledToastAnimation.fade,
        position: StyledToastPosition.center,
        context: Get.context);
  }

  static void showAlertDialog(String message) {
    final screenSize = Get.size;
    final maxWidth = screenSize.width * 0.6; // 屏幕宽度的60%
    final maxHeight = screenSize.height * 0.8; // 屏幕高度的80%

    Get.dialog(Center(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth, maxHeight: maxHeight),
        child: IntrinsicWidth(
          child: IntrinsicHeight(
            child: Container(
              constraints: const BoxConstraints(minWidth: 320, minHeight: 180),
              padding: const EdgeInsets.all(24),
              decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Expanded(
                    child: Container(
                      constraints: const BoxConstraints(minHeight: 40),
                      child: Center(
                        child: SingleChildScrollView(
                          child: Text(message,
                              textAlign: TextAlign.center, style: const TextStyle(fontSize: 14, height: 1.5, color: Color(0xFF333333))),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                  TextButton(
                    style: ButtonStyle(
                      minimumSize: MaterialStateProperty.all(const Size(120, 36)),
                      padding: MaterialStateProperty.all(const EdgeInsets.symmetric(horizontal: 24, vertical: 8)),
                      backgroundColor: MaterialStateProperty.all(const Color(0xFF2a82f5)),
                      shape:
                          MaterialStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(4))),
                    ),
                    onPressed: () => Get.back(),
                    child: const Text("确定", style: TextStyle(color: Colors.white, fontSize: 14)),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    ));
  }
}
