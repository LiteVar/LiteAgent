import 'package:flutter/material.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'package:lite_agent_client/widgets/tool_binding_popover.dart';

/// 工具函数项组件
/// 负责显示单个工具函数的详细信息，包括名称、描述、操作按钮等
class ToolFunctionItem extends StatelessWidget {
  final ToolFunctionModel function;
  final bool isAutoAgent;
  final bool isHovered;
  final VoidCallback onRemove;
  final VoidCallback onShowBinding;
  final VoidCallback onUnbind;
  final VoidCallback? onHoverEnter;
  final VoidCallback? onHoverExit;

  const ToolFunctionItem({
    super.key,
    required this.function,
    required this.isAutoAgent,
    required this.isHovered,
    required this.onRemove,
    required this.onShowBinding,
    required this.onUnbind,
    this.onHoverEnter,
    this.onHoverExit,
  });

  @override
  Widget build(BuildContext context) {
    String functionName = function.toolName;
    if ((function.requestMethod ?? "").isNotEmpty || function.functionName.isNotEmpty) {
      functionName = "${function.toolName}/${function.requestMethod?.toUpperCase()}${function.functionName.replaceFirst("/", "_")}";
    }

    return MouseRegion(
      onEnter: (event) => onHoverEnter?.call(),
      onExit: (event) => onHoverExit?.call(),
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: isHovered ? const Color(0xfff5f5f5) : Colors.transparent,
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              padding: const EdgeInsets.all(10),
              margin: const EdgeInsets.only(right: 10),
              decoration: BoxDecoration(color: const Color(0xffe8e8e8), borderRadius: BorderRadius.circular(4)),
              child: buildAssetImage("icon_default_tool.png", 0, Colors.black),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(functionName,
                      style: const TextStyle(fontSize: 14, color: Color(0xff333333)), maxLines: 1, overflow: TextOverflow.ellipsis),
                  Text(function.functionDescription,
                      style: const TextStyle(fontSize: 14, color: Color(0xff999999)), maxLines: 1, overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
            if (!isAutoAgent) ...[
              const SizedBox(width: 10),
              //暂时不做
              // ToolBindingPopover(
              //   function: function,
              //   onOpenBindingDialog: (func) => onShowBinding(),
              //   onUnbindAgent: (func) => onUnbind(),
              // ),
              // const SizedBox(width: 20),
              InkWell(onTap: onRemove, child: buildAssetImage("icon_delete.png", 20, Colors.black)),
            ],
          ],
        ),
      ),
    );
  }
}
