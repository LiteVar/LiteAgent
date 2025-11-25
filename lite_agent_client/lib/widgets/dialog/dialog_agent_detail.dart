import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/routes.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/extension/agent_extension.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';
import 'package:lite_agent_client/utils/web_util.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../config/constants.dart';
import '../../utils/agent/agent_validator.dart';
import '../../repositories/agent_repository.dart';
import '../../utils/event_bus.dart';

class AgentDetailDialog extends StatelessWidget {
  final AgentModel agent;
  bool needMoreButton = false;
  final logic = Get.put(AgentDetailDialogController());

  AgentDetailDialog({required this.agent}) {
    needMoreButton = logic.countLines(agent.description) > 5;
  }

  @override
  Widget build(BuildContext context) {
    return Center(
        child: Container(
            width: 538,
            height: 488,
            decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
            child: Column(children: [
              _buildTitleContainer(),
              Expanded(
                child: SingleChildScrollView(
                  child: Container(
                      margin: const EdgeInsets.all(20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Center(
                            child: Column(
                              children: [
                                const SizedBox(height: 10),
                                SizedBox(width: 82, height: 82, child: buildAgentProfileImage(agent.iconPath)),
                                Container(
                                    margin: const EdgeInsets.all(12),
                                    child: Text(agent.name, style: const TextStyle(fontSize: 14, color: Colors.black))),
                              ],
                            ),
                          ),
                          const Text("描述:", style: TextStyle(fontSize: 14, color: Colors.black)),
                          const SizedBox(height: 10),
                          Obx(() {
                            int? lines = needMoreButton && !logic.checkMore.value ? 5 : null;
                            String description = agent.description.isNotEmpty ? agent.description : "还没有添加描述";
                            return Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(description,
                                    softWrap: true, maxLines: lines, style: const TextStyle(fontSize: 14, color: Colors.grey)),
                                Offstage(
                                  offstage: !needMoreButton,
                                  child: Container(
                                    margin: const EdgeInsets.only(top: 5),
                                    child: InkWell(
                                      onTap: () => logic.checkMore.value = !logic.checkMore.value,
                                      child: Text(
                                        logic.checkMore.value ? "收起" : "查看更多",
                                        style: const TextStyle(fontSize: 14, color: Colors.blue),
                                      ),
                                    ),
                                  ),
                                )
                              ],
                            );
                          }),
                          const SizedBox(height: 20),
                          buildBottomButton(),
                        ],
                      )),
                ),
              )
            ])));
  }

  Widget buildBottomButton() {
    return Center(
        child: Column(children: [
      TextButton(
          style: ButtonStyle(
              padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(60, 5, 60, 5)),
              backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
              shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(2)),
              )),
          onPressed: () async {
            var isCloudAgent = agent.isCloud ?? false;
            AgentModel? targetAgent;
            if (isCloudAgent) {
              var agentDetail = await agentRepository.getCloudAgentDetail(agent.id);
              if (agentDetail?.model == null) {
                AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
                return;
              }
              if (agentDetail?.agent?.type == AgentValidator.DTO_TYPE_REFLECTION) {
                AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
                return;
              }
              if (agentDetail?.agent != null) {
                targetAgent = agentDetail!.agent!.toModel();
              }
            } else {
              String modelId = agent.modelId;
              var model = await modelRepository.getModelFromBox(modelId);
              if (model == null) {
                AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
                return;
              }
              if (agent.agentType == AgentValidator.DTO_TYPE_REFLECTION) {
                AlarmUtil.showAlertToast("反思Agent不能进行聊天对话");
                return;
              }
              targetAgent = await agentRepository.getAgentFromBox(agent.id);
            }
            eventBus.fire(AgentMessageEvent(message: EventBusMessage.startChat, agent: targetAgent));
            Get.back();
          },
          child: const Text('开始聊天', style: TextStyle(color: Colors.white, fontSize: 14))),
      const SizedBox(height: 20),
      TextButton(
          style: ButtonStyle(
              padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(60, 5, 60, 5)),
              shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)),
              )),
          onPressed: () {
            if (agent.isCloud ?? false) {
              Get.back();
              WebUtil.openAgentAdjustUrl(agent.id);
            } else {
              jumpToAdjustPage();
            }
          }.throttle(),
          child: const Text('进入调试', style: TextStyle(color: Color(0xFF999999), fontSize: 14)))
    ]));
  }

  Container _buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
          color: Color(0xFFf5f5f5), borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6))),
      child: Row(children: [
        const Text("agent详情"),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () => Get.back(),
        )
      ]),
    );
  }

  void jumpToAdjustPage() {
    Get.back();
    Get.toNamed(Routes.adjustment, arguments: agent);
  }
}

class AgentDetailDialogController extends GetxController {
  var checkMore = false.obs;

  int countLines(String text) {
    TextStyle style = const TextStyle(fontSize: 14);

    TextPainter textPainter = TextPainter(textDirection: TextDirection.ltr);

    textPainter.text = TextSpan(text: text, style: style);
    textPainter.layout(maxWidth: 498); //538-20-20

    int lineCount = textPainter.computeLineMetrics().length;

    return lineCount;
  }
}
