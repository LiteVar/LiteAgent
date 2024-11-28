import 'dart:io';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/routes.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/web_util.dart';

import '../../utils/event_bus.dart';

class AgentDetailDialog extends StatelessWidget {
  final AgentBean agent;
  final logic = Get.put(AgentDetailDialogController());

  AgentDetailDialog({required this.agent});

  @override
  Widget build(BuildContext context) {
    return Center(
        child: Container(
            width: 538,
            height: 488,
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.all(Radius.circular(6)),
            ),
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
                              if (agent.iconPath.isEmpty)
                                Container(
                                    width: 82,
                                    height: 82,
                                    decoration: BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.circular(6)))
                              else
                                Image(image: FileImage(File(agent.iconPath)), height: 82, width: 82, fit: BoxFit.cover),
                              Container(
                                  margin: const EdgeInsets.all(18),
                                  child: Text(agent.name, style: const TextStyle(fontSize: 14, color: Colors.black))),
                            ],
                          )),
                          const Text("描述:", style: TextStyle(fontSize: 14, color: Colors.black)),
                          const SizedBox(height: 10),
                          Obx(() {
                            int? lines = logic.showMore.value ? 5 : null;
                            String description = agent.description.isNotEmpty ? agent.description : "还没有添加描述";
                            return Text(description,
                                softWrap: true, maxLines: lines, style: const TextStyle(fontSize: 14, color: Colors.grey));
                          }),
                          const SizedBox(height: 30),
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
            String modelId = agent.modelId;
            var model = await modelRepository.getModelFromBox(modelId);
            if (model == null) {
              AlarmUtil.showAlertDialog("没有设置模型，无法进行聊天");
              return;
            }
            eventBus.fire(AgentMessageEvent(message: EventBusMessage.startChat, agent: agent));
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
          },
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
  var showMore = false.obs;
}
