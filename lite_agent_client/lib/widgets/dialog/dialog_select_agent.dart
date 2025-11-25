import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../models/dto/agent.dart';

class SelectAgentDialog extends StatelessWidget {
  final void Function(AgentModel agent) onStartChatConfirm;

  SelectAgentDialog({super.key, required this.onStartChatConfirm});

  static const String TAB_SEC_ALL = "secondary_all";
  static const String TAB_SEC_SYSTEM = "secondary_system";
  static const String TAB_SEC_SHARE = "secondary_share";
  static const String TAB_SEC_MINE = "secondary_mine";

  var currentSecondaryTab = TAB_SEC_ALL.obs;

  var currentAgentList = <AgentModel>[].obs;
  final _agentListMap = <String, List<AgentModel>>{};

  Future<void> initData() async {
    _agentListMap[TAB_SEC_ALL] ??= [];
    _agentListMap[TAB_SEC_ALL]?.addAll((await agentRepository.getAgentListFromBox()));
    _agentListMap[TAB_SEC_ALL]?.addAll(await agentRepository.getCloudAgentListAndTranslate(0));
    _agentListMap[TAB_SEC_SYSTEM] = (await agentRepository.getCloudAgentListAndTranslate(1));
    _agentListMap[TAB_SEC_SHARE] = (await agentRepository.getCloudAgentListAndTranslate(2));
    _agentListMap[TAB_SEC_MINE] = (await agentRepository.getCloudAgentListAndTranslate(3));
    switchSecondaryTab(TAB_SEC_ALL);
  }

  void switchSecondaryTab(String tabType) {
    currentSecondaryTab.value = tabType;
    currentAgentList.value = _agentListMap[tabType]!;
    currentAgentList.refresh();
  }

  @override
  Widget build(BuildContext context) {
    initData();
    return Center(
        child: Container(
            width: 586,
            height: 560,
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.all(Radius.circular(6)),
            ),
            child: Column(children: [
              buildTitleContainer(),
              const SizedBox(height: 10),
              _buildTabContainer(),
              Expanded(child: _buildAgentListView()),
              const SizedBox(height: 10)
              //_buildAgentListView()
            ])));
  }

  Widget _buildTabContainer() {
    return Obx(() {
      var allColor = currentSecondaryTab.value == TAB_SEC_ALL ? Colors.black : Colors.grey;
      var sysColor = currentSecondaryTab.value == TAB_SEC_SYSTEM ? Colors.black : Colors.grey;
      var shareColor = currentSecondaryTab.value == TAB_SEC_SHARE ? Colors.black : Colors.grey;
      var meColor = currentSecondaryTab.value == TAB_SEC_MINE ? Colors.black : Colors.grey;
      return Row(children: [
        TextButton(onPressed: () => switchSecondaryTab(TAB_SEC_ALL), child: Text('全部', style: TextStyle(fontSize: 16, color: allColor))),
        TextButton(onPressed: () => switchSecondaryTab(TAB_SEC_SYSTEM), child: Text('系统', style: TextStyle(fontSize: 16, color: sysColor))),
        TextButton(
            onPressed: () => switchSecondaryTab(TAB_SEC_SHARE), child: Text('分享', style: TextStyle(fontSize: 16, color: shareColor))),
        TextButton(onPressed: () => switchSecondaryTab(TAB_SEC_MINE), child: Text('我的', style: TextStyle(fontSize: 16, color: meColor)))
      ]);
    });
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        const Text("新的聊天"),
        const Spacer(),
        IconButton(onPressed: () => Get.back(), icon: const Icon(Icons.close, size: 16, color: Colors.black))
      ]),
    );
  }

  Widget _buildAgentListView() {
    return Obx(() {
      return ListView.builder(
          itemCount: currentAgentList.length,
          itemBuilder: (context, index) {
            var agent = currentAgentList[index];
            return _buildAgentItem(agent);
          });
    });
  }

  Container _buildAgentItem(AgentModel agent) {
    var iconPath = agent.iconPath;
    var description = agent.description;
    if (description.contains("\n")) {}
    return Container(
        margin: const EdgeInsets.all(10),
        child: Column(
          children: [
            Container(
              margin: const EdgeInsets.fromLTRB(10, 15, 10, 0),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(margin: const EdgeInsets.only(bottom: 10), width: 48, height: 48, child: buildAgentProfileImage(iconPath)),
                  Expanded(
                    child: Container(
                      margin: const EdgeInsets.fromLTRB(10, 0, 10, 10),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(children: [
                          Text(agent.name ?? "", style: const TextStyle(fontSize: 16, color: Colors.black)),
                          Offstage(
                              offstage: !(agent.isCloud ?? false),
                              child: Container(
                                margin: const EdgeInsets.symmetric(horizontal: 8),
                                child: buildAssetImage("icon_cloud.png", 16, Colors.grey),
                              ))
                        ]),
                        Text(description, style: const TextStyle(fontSize: 14, color: Colors.grey))
                      ]),
                    ),
                  ),
                  Container(
                      margin: const EdgeInsets.symmetric(vertical: 14),
                      child: TextButton(
                          style: ButtonStyle(
                              padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 15)),
                              backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                              shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                                RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                              )),
                          onPressed: () => onStartChatConfirm(agent),
                          child: const Text('开始聊天', style: TextStyle(color: Colors.white, fontSize: 14))))
                ],
              ),
            ),
            const Divider(height: 0.1)
          ],
        ));
  }
}
