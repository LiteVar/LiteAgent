import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

class ToolBindingDialog extends StatelessWidget {
  final ToolFunctionModel function;
  final List<AgentModel> availableAgents;
  final Function(ToolFunctionModel, AgentModel?, String?) onConfirm;

  const ToolBindingDialog({
    super.key, 
    required this.function, 
    required this.availableAgents, 
    required this.onConfirm
  });

  void _onConfirmPressed(Rx<AgentModel?> selectedAgent, Rx<String?> triggerMethod) {
    onConfirm(function, selectedAgent.value, triggerMethod.value);
    Get.back();
  }

  @override
  Widget build(BuildContext context) {
    final selectedAgent = Rx<AgentModel?>(null);
    final triggerMethod = Rx<String?>(null);

    // 如果已绑定，设置默认值
    if (function.isBound) {
      selectedAgent.value = availableAgents.firstWhereOrNull((agent) => agent.id == function.boundAgentId);
      triggerMethod.value = function.triggerMethod;
    }

    return Dialog(
      child: Container(
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(8))),
        width: 480,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildTitleContainer(),
            horizontalLine(),
            // 内容区域
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("请为工具名称/方法选择要绑定的agent", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
                  const SizedBox(height: 20),
                  _buildAgentSelectRow(selectedAgent),
                  const SizedBox(height: 16),
                  _buildTriggerModeRow(triggerMethod),
                  const SizedBox(height: 24),
                  _buildBottomButtonRow(selectedAgent, triggerMethod),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Row _buildBottomButtonRow(Rx<AgentModel?> selectedAgent, Rx<String?> triggerMethod) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Container(
          height: 32,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          decoration: BoxDecoration(
            border: Border.all(color: const Color(0xffd9d9d9)),
            borderRadius: BorderRadius.circular(4),
          ),
          child: InkWell(
            onTap: () => Get.back(),
            child: const Center(child: Text("取消", style: TextStyle(fontSize: 14, color: Color(0xff333333)))),
          ),
        ),
        const SizedBox(width: 12),
        Obx(() => Container(
              height: 32,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                  color: selectedAgent.value != null && triggerMethod.value != null ? const Color(0xff2A82E4) : const Color(0xffd9d9d9),
                  borderRadius: BorderRadius.circular(4)),
              child: InkWell(
                onTap: selectedAgent.value != null && triggerMethod.value != null
                    ? () => _onConfirmPressed(selectedAgent, triggerMethod)
                    : null,
                child: const Center(child: Text("确定", style: TextStyle(fontSize: 14, color: Colors.white))),
              ),
            )),
      ],
    );
  }

  Row _buildTriggerModeRow(Rx<String?> triggerMethod) {
    return Row(
      children: [
        const SizedBox(width: 100, child: Text("触发方式", style: TextStyle(fontSize: 14, color: Color(0xff333333)))),
        Expanded(
          child: Obx(() => DropdownButtonHideUnderline(
                child: DropdownButton2<String>(
                  isExpanded: true,
                  hint: const Text("请选择触发方式", style: TextStyle(fontSize: 14)),
                  value: triggerMethod.value,
                  items: const [
                    DropdownMenuItem<String>(
                      value: "先执行agent",
                      child: Text("先执行agent", style: TextStyle(fontSize: 14)),
                    ),
                    DropdownMenuItem<String>(
                      value: "先执行工具",
                      child: Text("先执行工具", style: TextStyle(fontSize: 14)),
                    ),
                    DropdownMenuItem<String>(
                      value: "同时agent和工具执行",
                      child: Text("同时agent和工具执行", style: TextStyle(fontSize: 14)),
                    ),
                  ],
                  onChanged: (value) => triggerMethod.value = value,
                  dropdownStyleData: const DropdownStyleData(decoration: BoxDecoration(color: Colors.white)),
                  customButton: Container(
                    height: 32,
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(border: Border.all(color: const Color(0xffd9d9d9)), borderRadius: BorderRadius.circular(4)),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(triggerMethod.value ?? "请选择触发方式",
                              style: TextStyle(
                                  fontSize: 14, color: triggerMethod.value != null ? const Color(0xff333333) : const Color(0xff999999))),
                        ),
                        const Icon(Icons.keyboard_arrow_down, size: 16, color: Color(0xff999999)),
                      ],
                    ),
                  ),
                ),
              )),
        ),
      ],
    );
  }

  Row _buildAgentSelectRow(Rx<AgentModel?> selectedAgent) {
    return Row(
      children: [
        const SizedBox(width: 100, child: Text("绑定的agent", style: TextStyle(fontSize: 14, color: Color(0xff333333)))),
        Expanded(
          child: Obx(() => DropdownButtonHideUnderline(
                child: DropdownButton2<AgentModel>(
                  isExpanded: true,
                  hint: const Text("请选择agent", style: TextStyle(fontSize: 14)),
                  value: selectedAgent.value,
                  items: availableAgents
                      .map((agent) => DropdownMenuItem<AgentModel>(
                            value: agent,
                            child: Row(
                              children: [
                                SizedBox(width: 24, height: 24, child: buildAgentProfileImage(agent.iconPath)),
                                const SizedBox(width: 8),
                                Text(agent.name, style: const TextStyle(fontSize: 14)),
                              ],
                            ),
                          ))
                      .toList(),
                  onChanged: (value) => selectedAgent.value = value,
                  dropdownStyleData: const DropdownStyleData(maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
                  customButton: Container(
                    height: 32,
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(border: Border.all(color: const Color(0xffd9d9d9)), borderRadius: BorderRadius.circular(4)),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            selectedAgent.value?.name ?? "请选择agent",
                            style: TextStyle(
                                fontSize: 14, color: selectedAgent.value != null ? const Color(0xff333333) : const Color(0xff999999)),
                          ),
                        ),
                        const Icon(Icons.keyboard_arrow_down, size: 16, color: Color(0xff999999)),
                      ],
                    ),
                  ),
                ),
              )),
        ),
      ],
    );
  }

  Container _buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      decoration: const BoxDecoration(borderRadius: BorderRadius.only(topLeft: Radius.circular(8), topRight: Radius.circular(8))),
      child: Row(
        children: [
          const Text("绑定agent", style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500, color: Color(0xff333333))),
          const Spacer(),
          InkWell(onTap: () => Get.back(), child: const Icon(Icons.close, size: 16, color: Color(0xff333333))),
        ],
      ),
    );
  }
}
