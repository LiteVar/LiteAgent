import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

/// 工具执行模式选择器组件
/// 负责显示和选择工具的执行模式（并行/串行/拒绝）
class ToolOperationModeSelector extends StatelessWidget {
  final int currentMode;
  final Function(int) onModeChanged;

  const ToolOperationModeSelector({
    super.key,
    required this.currentMode,
    required this.onModeChanged,
  });

  @override
  Widget build(BuildContext context) {
    return DropdownButtonHideUnderline(
      child: DropdownButton2(
        customButton: buildClickButton("icon_down.png", "更多", null),
        isExpanded: true,
        dropdownStyleData: const DropdownStyleData(
          width: 130,
          offset: Offset(-10, -8),
          padding: EdgeInsets.all(0),
          decoration: BoxDecoration(color: Colors.white),
        ),
        menuItemStyleData: MenuItemStyleData(
          height: 40,
          padding: const EdgeInsets.all(0),
          overlayColor: WidgetStateProperty.resolveWith<Color>((Set<WidgetState> states) => Colors.transparent),
        ),
        items: [_buildToolOperationModeMenuItem(context)],
        onChanged: (value) {},
      ),
    );
  }

  DropdownMenuItem<String> _buildToolOperationModeMenuItem(BuildContext context) {
    return DropdownMenuItem<String>(
      value: "operationMode",
      child: DropdownButtonHideUnderline(
        child: DropdownButton2(
          customButton: SizedBox(
            height: 40,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text("工具执行模式", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                const SizedBox(width: 5),
                buildAssetImage("icon_right.png", 12, const Color(0xff2A82E4)),
              ],
            ),
          ),
          dropdownStyleData: const DropdownStyleData(
              width: 80, offset: Offset(135, 40), padding: EdgeInsets.all(0), decoration: BoxDecoration(color: Colors.white)),
          menuItemStyleData: const MenuItemStyleData(height: 40),
          items: [
            DropdownMenuItem<String>(
              value: "parallel",
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Visibility(
                    visible: currentMode == OperationMode.PARALLEL,
                    child: buildAssetImage("icon_checked.png", 12, const Color(0xff2A82E4)),
                  ),
                  const SizedBox(width: 5),
                  const Text("并行", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                ],
              ),
            ),
            DropdownMenuItem<String>(
              value: "serial",
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Visibility(
                    visible: currentMode == OperationMode.SERIAL,
                    child: buildAssetImage("icon_checked.png", 12, const Color(0xff2A82E4)),
                  ),
                  const SizedBox(width: 5),
                  const Text("串行", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                ],
              ),
            ),
            // TODO: server支持单个方法的排他性后再支持此选项
            // DropdownMenuItem<String>(
            //     value: "reject",
            //     child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
            //       Visibility(
            //           visible: currentMode == OperationMode.REJECT,
            //           child: buildAssetImage("icon_checked.png", 12, const Color(0xff2A82E4))),
            //       const SizedBox(width: 5),
            //       const Text("拒绝", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
            //     ])),
          ],
          onChanged: (value) {
            if (value == "parallel") {
              onModeChanged(OperationMode.PARALLEL);
            } else if (value == "serial") {
              onModeChanged(OperationMode.SERIAL);
            } else if (value == "reject") {
              onModeChanged(OperationMode.REJECT);
            }
            // 选择后关闭外层“工具执行模式”菜单
            // 使用下一帧尝试关闭外层 Dropdown 的菜单路由
            WidgetsBinding.instance.addPostFrameCallback((_) {
              final navigator = Navigator.of(context);
              if (navigator.canPop()) {
                navigator.pop();
              }
            });
          },
        ),
      ),
    );
  }

  Widget buildClickButton(String fileName, String text, Function()? onTap) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 12),
        decoration: const BoxDecoration(color: Color(0xffe7f2fe), borderRadius: BorderRadius.all(Radius.circular(8))),
        child: Row(
          children: [
            buildAssetImage(fileName, 14, const Color(0xff2A82E4)),
            const SizedBox(width: 5),
            Text(text, style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
          ],
        ),
      ),
    );
  }
}
