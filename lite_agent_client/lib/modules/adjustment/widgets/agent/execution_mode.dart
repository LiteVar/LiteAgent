import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'package:lite_agent_client/widgets/common_dropdown.dart';
import 'agent_state_manager.dart';

/// 执行模式组件
/// 负责Agent执行模式的详细配置
class ExecutionMode extends StatelessWidget {
  final AgentStateManager agentStateManager;
  final VoidCallback? onDataChanged;

  const ExecutionMode({
    super.key,
    required this.agentStateManager,
    this.onDataChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 10),
        InkWell(
          onTap: () => agentStateManager.toggleExecutionModeExpanded(!agentStateManager.isExecutionModeExpanded.value),
          child: Row(children: [
            Obx(() => buildAssetImage(
                agentStateManager.isExecutionModeExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png",
                18,
                const Color(0xff333333))),
            const Text("执行模式", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
          ]),
        ),
        Obx(() => Offstage(
              offstage: !agentStateManager.isExecutionModeExpanded.value,
              child: Container(
                margin: const EdgeInsets.only(left: 18),
                child:  _buildAgentOptionColumn(),
              ),
            )),
        const SizedBox(height: 10),
        horizontalLine(),
      ],
    );
  }

  Widget _buildAgentOptionColumn() {
    return Column(children: [
      const SizedBox(height: 10),
      Row(
        children: [
          const Text("Agent类型", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
          const Spacer(),
          Obx(() => SimpleDropdown<String>(
                selectedValue: agentStateManager.agentType.value.toString(),
                items: const [
                  DropdownItem<String>(value: "0", text: "普通"),
                  DropdownItem<String>(value: "2", text: "反思"),
                ],
                placeholder: "请选择Agent类型",
                width: 220,
                onChanged: (value) {
                  if (value != null) {
                    agentStateManager.updateAgentType(int.parse(value));
                    onDataChanged?.call();
                  }
                },
              )),
        ],
      ),
      const SizedBox(height: 4),
      Row(
        children: [
          const Text("执行模式", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
          const Spacer(),
          Obx(() => SimpleDropdown<String>(
                selectedValue: _getOperationModeString(agentStateManager.operationMode.value),
                items: const [
                  DropdownItem<String>(value: "parallel", text: "并行"),
                  DropdownItem<String>(value: "serial", text: "串行"),
                  DropdownItem<String>(value: "reject", text: "拒绝"),
                ],
                placeholder: "请选择执行模式",
                width: 220,
                onChanged: (value) {
                  if (value != null) {
                    int mode = _getOperationModeFromString(value);
                    agentStateManager.updateOperationMode(mode);
                    onDataChanged?.call();
                  }
                },
              )),
        ],
      ),
    ]);
  }

  String _getOperationModeString(int mode) {
    switch (mode) {
      case 0:
        return "parallel";
      case 1:
        return "serial";
      case 2:
        return "reject";
      default:
        return "parallel";
    }
  }

  int _getOperationModeFromString(String mode) {
    switch (mode) {
      case "parallel":
        return 0;
      case "serial":
        return 1;
      case "reject":
        return 2;
      default:
        return 0;
    }
  }
}
