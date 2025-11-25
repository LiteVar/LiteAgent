import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'package:lite_agent_client/utils/agent/agent_validator.dart';
import 'agent_state_manager.dart';

/// 子Agent列表组件
/// 负责子Agent的添加、显示和管理
class ChildAgentList extends StatelessWidget {
  final AgentStateManager agentStateManager;
  final VoidCallback? onDataChanged;
  final VoidCallback? onShowChildAgentSelectDialog;

  const ChildAgentList({
    super.key,
    required this.agentStateManager,
    this.onDataChanged,
    this.onShowChildAgentSelectDialog,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: const EdgeInsets.only(top: 10),
          child: Row(
            children: [
              InkWell(
                onTap: () => agentStateManager.toggleChildAgentsExpanded(!agentStateManager.isChildAgentsExpanded.value),
                child: Row(children: [
                  Obx(() => buildAssetImage(
                    agentStateManager.isChildAgentsExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 
                    18,
                    const Color(0xff333333)
                  )),
                  const Text("子Agent设置", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
                ]),
              ),
              const Spacer(),
              buildClickButton("icon_add.png", "添加", onShowChildAgentSelectDialog),
            ],
          ),
        ),
        const SizedBox(height: 10),
        Obx(() => Offstage(
          offstage: !agentStateManager.isChildAgentsExpanded.value,
          child: Container(
            margin: const EdgeInsets.only(left: 10),
            child: Column(children: [
              if (agentStateManager.childAgentList.isNotEmpty)
                ...List.generate(
                  agentStateManager.childAgentList.length,
                  (index) => _buildChildAgentItem(index, agentStateManager.childAgentList[index]),
                )
            ])),
        )),
      ],
    );
  }

  Widget _buildChildAgentItem(int index, AgentModel agent) {
    return MouseRegion(
      onEnter: (event) => agentStateManager.hoverAgentItem(index.toString()),
      onExit: (event) => agentStateManager.hoverAgentItem(""),
      child: Obx(() {
        var isSelect = agentStateManager.agentHoverItemId.value == index.toString();
        var backgroundColor = isSelect ? const Color(0xfff5f5f5) : Colors.transparent;
        return Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(color: backgroundColor, borderRadius: BorderRadius.circular(8)),
          child: Row(children: [
            SizedBox(width: 30, height: 30, child: buildAgentProfileImage(agent.iconPath)),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                agent.name,
                style: const TextStyle(fontSize: 14, color: Colors.black),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 10),
              child: Text(
                agent.agentType == AgentValidator.DTO_TYPE_REFLECTION ? "反思" : "普通",
                style: const TextStyle(fontSize: 14, color: Color(0xff999999)),
              ),
            ),
            InkWell(
              onTap: () => _removeChildAgent(index),
              child: Container(
                padding: const EdgeInsets.all(4),
                child: buildAssetImage("icon_delete.png", 16, const Color(0xff999999)),
              ),
            ),
          ]));
      }),
    );
  }

  void _removeChildAgent(int index) {
    agentStateManager.removeChildAgent(index);
    onDataChanged?.call();
  }

  InkWell buildClickButton(String fileName, String text, Function()? onTap) {
    return InkWell(
        onTap: onTap,
        child: Container(
            padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 12),
            decoration: const BoxDecoration(color: Color(0xffe7f2fe), borderRadius: BorderRadius.all(Radius.circular(8))),
            child: Row(
              children: [
                buildAssetImage(fileName, 14, const Color(0xff2A82E4)),
                const SizedBox(width: 5),
                Text(text, style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
              ],
            )));
  }
}
