import 'package:flutter/material.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

/// 通用点击按钮组件
/// 提供统一的按钮样式和交互
class CommonBlueButton extends StatelessWidget {
  final String iconName;
  final String buttonText;
  final Function()? onTap;

  const CommonBlueButton({
    super.key,
    required this.iconName,
    required this.buttonText,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 12),
        decoration: const BoxDecoration(color: Color(0xffe7f2fe), borderRadius: BorderRadius.all(Radius.circular(8))),
        child: Row(
          children: [
            buildAssetImage(iconName, 14, const Color(0xff2A82E4)),
            const SizedBox(width: 5),
            Text(buttonText, style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
          ],
        ),
      ),
    );
  }
}
