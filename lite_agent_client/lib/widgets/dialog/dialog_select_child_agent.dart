import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';

import '../../models/local_data_model.dart';
import '../../repositories/model_repository.dart';
import '../../utils/alarm_util.dart';
import '../common_widget.dart';

class SelectChildAgentDialog extends StatelessWidget {
  SelectChildAgentDialog({super.key, required this.currentAgentId, required this.selectAgents, this.onSelectChanged});

  //ui
  var borderRadius = 16.0;

  //data
  var currentAgentId = "";
  var selectAgents = <AgentBean>[];
  var agentList = <AgentBean>[].obs;
  var noModelList = <String>[];
  Function()? onSelectChanged;

  Future<void> initData() async {
    var list = (await agentRepository.getAgentListFromBox()).toList();
    list.removeWhere((agent) => agent.autoAgentFlag ?? false);
    for (var agent in list) {
      var model = await modelRepository.getModelFromBox(agent.modelId);
      if (model == null && agent.id != currentAgentId) {
        noModelList.add(agent.id);
      }
    }
    //list.removeWhere((agent) => noModelList.contains(agent.id));
    agentList.assignAll(list);
  }

  Future<void> toggleSelectStatus(AgentBean agent) async {
    var isSelected = false;
    for (var selectAgent in selectAgents) {
      if (selectAgent.id == agent.id) {
        isSelected = true;
        selectAgents.remove(selectAgent);
        break;
      }
    }
    if (!isSelected) {
      var childAgentIdList = await getAllChildAgentIdList(agent.id);
      if (childAgentIdList.contains(currentAgentId)) {
        AlarmUtil.showAlertToast("当前Agent为该Agent的子Agent");
        return;
      }

      if (agent.agentType == AgentType.REFLECTION && selectAgents.length >= 5) {
        var refectionAgentNumber = 0;
        for (var agent in agentList) {
          if (isAgentSelected(agent) && agent.agentType == AgentType.REFLECTION) {
            refectionAgentNumber++;
          }
          if (refectionAgentNumber >= 5) {
            AlarmUtil.showAlertToast("反思agent个数不能超过5个");
            return;
          }
        }
      }
      selectAgents.add(agent);
      onSelectChanged?.call();
    }
    agentList.refresh();
  }

  Future<List<String>> getAllChildAgentIdList(String agentId) async {
    var list = <String>[];
    var agent = await agentRepository.getAgentFromBox(agentId);
    var childAgentIdList = agent?.childAgentIds?.toList() ?? [];
    list.addAll(childAgentIdList);
    for (var agentId in childAgentIdList) {
      list.addAll(await getAllChildAgentIdList(agentId));
    }
    return list;
  }

  bool isAgentSelected(AgentBean agent) {
    for (var selectAgent in selectAgents) {
      if (selectAgent.id == agent.id) {
        return true;
      }
    }
    return false;
  }

  @override
  Widget build(BuildContext context) {
    initData();
    return Center(
        child: Container(
            width: 586,
            height: 560,
            padding: const EdgeInsets.only(bottom: 10),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(borderRadius))),
            child: Column(children: [
              buildTitleContainer(),
              Expanded(
                  child: Obx(() => ListView.builder(
                        itemCount: agentList.length,
                        itemBuilder: (context, index) => buildListItem(agentList[index]),
                      ))),
            ])));
  }

  Container buildTitleContainer() {
    return Container(
      margin: const EdgeInsets.only(bottom: 15),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(borderRadius), topRight: Radius.circular(borderRadius)),
      ),
      child: Row(children: [
        const Text("子Agent设置"),
        const Spacer(),
        InkWell(
            onTap: () => Get.back(),
            child: Container(margin: const EdgeInsets.only(right: 2), child: buildAssetImage("icon_close.png", 16, Colors.black)))
      ]),
    );
  }

  Container buildListItem(AgentBean agent) {
    var isSelected = isAgentSelected(agent);
    var buttonText = isSelected ? "移除" : "添加";
    var typeString = "";
    if (agent.agentType == AgentType.DISTRIBUTE) {
      typeString = "分发";
    } else if (agent.agentType == AgentType.REFLECTION) {
      typeString = "反思";
    } else {
      typeString = "普通";
    }
    var buttonTextColor = isSelected ? const Color(0xffF24E4E) : const Color(0xff2A82E4);
    var buttonColor = isSelected ? const Color(0xffFFE8E8) : const Color(0xffE8F2FF);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        children: [
          Row(
            children: [
              SizedBox(width: 42, height: 42, child: buildAgentProfileImage(agent.iconPath)),
              Expanded(
                child: Container(
                    margin: const EdgeInsets.symmetric(horizontal: 15),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(agent.name, style: const TextStyle(fontSize: 16, color: Colors.black)),
                      Text("类型：$typeString", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
                    ])),
              ),
              if (noModelList.contains(agent.id))
                const Text("未配置模型，无法添加", style: TextStyle(fontSize: 14, color: Color(0xff999999)))
              else if (agent.id == currentAgentId)
                const Text("当前Agent", style: TextStyle(fontSize: 14, color: Color(0xff999999)))
              else
                TextButton(
                    style: ButtonStyle(
                        padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 16)),
                        backgroundColor: WidgetStateProperty.all(buttonColor),
                        shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                          RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                        )),
                    onPressed: () => toggleSelectStatus(agent),
                    child: Text(buttonText, style: TextStyle(color: buttonTextColor, fontSize: 14)))
            ],
          ),
          Container(margin: const EdgeInsets.symmetric(vertical: 15), child: horizontalLine()),
        ],
      ),
    );
  }
}
