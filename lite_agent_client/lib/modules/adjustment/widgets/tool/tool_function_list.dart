import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_tool_function.dart';
import 'package:lite_agent_client/widgets/dialog/tool_binding_dialog.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'tool_function_item.dart';
import 'tool_operation_mode_selector.dart';
import 'tool_state_manager.dart';

/// 工具函数列表组件
/// 负责工具配置的整体展示，包括展开/收起、工具列表、操作按钮等
class ToolFunctionList extends StatelessWidget {
  final bool isAutoAgent;
  final ToolStateManager toolStateManager;
  final String currentAgentId;
  final VoidCallback onBackToToolPage;
  final VoidCallback? onDataChanged;

  const ToolFunctionList({
    super.key,
    required this.isAutoAgent,
    required this.toolStateManager,
    required this.currentAgentId,
    required this.onBackToToolPage,
    this.onDataChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          margin: const EdgeInsets.only(top: 10),
          child: Row(
            children: [
              InkWell(
                onTap: () => toolStateManager.toggleToolExpanded(!toolStateManager.isToolExpanded.value),
                child: Row(
                  children: [
                    Obx(() => buildAssetImage(toolStateManager.isToolExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png",
                        18, const Color(0xff333333))),
                    const Text("工具", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
                  ],
                ),
              ),
              const Spacer(),
              const SizedBox(width: 10),
              if (!isAutoAgent) ...[
                buildClickButton("icon_add.png", "添加", _showToolFunctionSelectDialog),
                const SizedBox(width: 10),
                Obx(() => ToolOperationModeSelector(
                      currentMode: toolStateManager.toolOperationMode.value,
                      onModeChanged: (mode) {
                        toolStateManager.updateToolOperationMode(mode);
                        onDataChanged?.call();
                      },
                    ))
              ],
            ],
          ),
        ),
        const SizedBox(height: 10),
        Obx(() => Offstage(
              offstage: !toolStateManager.isToolExpanded.value,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    margin: const EdgeInsets.only(left: 18),
                    child: Text(
                      isAutoAgent ? "在工具库中对工具开启\"支持 Auto Multi Agent\"后，工具将在此处显示。当指令调用工具时，将自动调用。" : "Agent在特定场景下可以调用工具，可以更好执行指令",
                      style: const TextStyle(color: Color(0xff999999), fontSize: 12),
                    ),
                  ),
                  Obx(() => Container(
                        margin: const EdgeInsets.only(top: 10, left: 10),
                        child: Column(
                          children: [
                            if (toolStateManager.hasFunctions)
                              ...List.generate(
                                toolStateManager.displayFunctionCount,
                                (index) => ToolFunctionItem(
                                  function: toolStateManager.functionList[index],
                                  isAutoAgent: isAutoAgent,
                                  isHovered: toolStateManager.toolHoverItemId.value == index.toString(),
                                  onRemove: () => toolStateManager.removeFunction(index),
                                  onShowBinding: () => _showToolBindingDialog(toolStateManager.functionList[index]),
                                  onUnbind: () => _unbindAgent(toolStateManager.functionList[index]),
                                  onHoverEnter: () => toolStateManager.hoverToolItem(index.toString()),
                                  onHoverExit: () => toolStateManager.hoverToolItem(""),
                                ),
                              ),
                          ],
                        ),
                      )),
                  Obx(() => Offstage(
                        offstage: !toolStateManager.shouldShowMoreButton,
                        child: InkWell(
                          onTap: () => toolStateManager.toggleShowMoreTool(!toolStateManager.showMoreTool.value),
                          child: Container(
                            margin: const EdgeInsets.symmetric(vertical: 10),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  toolStateManager.showMoreTool.value ? "收起" : "更多",
                                  style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4)),
                                ),
                                const SizedBox(width: 10),
                                buildAssetImage(
                                    toolStateManager.showMoreTool.value ? "icon_up.png" : "icon_down.png", 12, const Color(0xff2A82E4)),
                              ],
                            ),
                          ),
                        ),
                      )),
                  Offstage(
                    offstage: !(isAutoAgent && !toolStateManager.hasFunctions),
                    child: Container(
                      margin: const EdgeInsets.only(left: 18, bottom: 10),
                      child: Row(
                        children: [
                          const Text("还没添加可用工具，", style: TextStyle(color: Color(0xff666666), fontSize: 12)),
                          InkWell(
                            onTap: onBackToToolPage,
                            child: const Text("前往工具管理", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            )),
        horizontalLine(),
      ],
    );
  }

  /// 显示工具选择对话框
  void _showToolFunctionSelectDialog() {
    Get.dialog(
      barrierDismissible: false,
      SelectToolFunctionDialog(
        selectToolFunctionList: toolStateManager.functionList,
        onSelectChanged: () {
          toolStateManager.functionList.refresh();
          onDataChanged?.call();
        },
      ),
    );
  }

  /// 显示工具绑定对话框
  Future<void> _showToolBindingDialog(ToolFunctionModel function) async {
    var availableAgents = await _getAvailableAgents();

    Get.dialog(
      ToolBindingDialog(
        function: function,
        availableAgents: availableAgents,
        onConfirm: (func, agent, triggerMethod) {
          if (agent != null) {
            func.isBound = true;
            func.boundAgentId = agent.id;
            func.boundAgentName = agent.name;
            func.triggerMethod = triggerMethod;
            toolStateManager.functionList.refresh();
            onDataChanged?.call();
          }
        },
      ),
    );
  }

  /// 解绑Agent
  void _unbindAgent(ToolFunctionModel function) {
    function.isBound = false;
    function.boundAgentId = null;
    function.boundAgentName = null;
    function.triggerMethod = null;
    toolStateManager.functionList.refresh();
    onDataChanged?.call();
    AlarmUtil.showAlertToast("解绑成功");
  }

  /// 获取可用的Agent列表
  Future<List<AgentModel>> _getAvailableAgents() async {
    var allAgents = await agentRepository.getAgentListFromBox();
    var availableAgents = allAgents.where((agent) => agent.id != currentAgentId && !(agent.autoAgentFlag ?? false)).toList();
    return availableAgents;
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
