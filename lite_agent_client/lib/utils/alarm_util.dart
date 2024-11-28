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
    Get.dialog(Center(
      child: Container(
        width: 300,
        height: 180,
        padding: const EdgeInsets.symmetric(vertical: 20),
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Expanded(child: Center(child: Text(message))),
            TextButton(
                style: ButtonStyle(
                    padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                    backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                    shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(2)))),
                onPressed: () => Get.back(),
                child: const Text("确定", style: TextStyle(color: Colors.white, fontSize: 14)))
          ],
        ),
      ),
    ));
  }
}
