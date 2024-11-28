import 'dart:io';

import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/agent.dart';
import 'package:lite_agent_client/utils/web_util.dart';

import 'logic.dart';

class AgentPage extends StatelessWidget {
  AgentPage({Key? key}) : super(key: key);

  final logic = Get.put(AgentLogic());

  final buttonColor = const Color(0xFF2a82f5);
  final itemBorderColor = const Color(0xFFd9d9d9);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: Container(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
            color: Colors.white,
            child: Column(children: [
              _buildTitle(),
              Expanded(
                child: Stack(
                  children: [
                    Column(children: [
                      _buildSecondaryTitle(),
                      const SizedBox(height: 10),
                      buildListExpanded(),
                    ]),
                    buildLoginCover()
                  ],
                ),
              )
            ])));
  }

  Widget buildLoginCover() {
    return Obx(() => Offstage(
          offstage: logic.currentTab.value == AgentLogic.TAB_LOCAL || logic.isLogin,
          child: Container(
            color: Colors.white,
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(width: 79, height: 79, color: itemBorderColor),
                  const SizedBox(height: 20),
                  const Text("您需要登录后才可查看同步云端信息", style: TextStyle(fontSize: 12)),
                  const SizedBox(height: 20),
                  TextButton(
                      style: ButtonStyle(
                          padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(24, 18, 24, 18)),
                          backgroundColor: WidgetStateProperty.all(buttonColor),
                          shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                            RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(10),
                            ),
                          )),
                      onPressed: () => logic.onLoginButtonClick(),
                      child: const Text('登录', style: TextStyle(color: Colors.white, fontSize: 14)))
                ],
              ),
            ),
          ),
        ));
  }

  Expanded buildListExpanded() {
    return Expanded(
      child: Obx(() {
        return GridView.count(
            crossAxisCount: 4,
            childAspectRatio: 5 / 4,
            children: List.generate(
                logic.currentAgentList.length,
                (index) => InkWell(
                    onTap: () => logic.showAgentDetailDialog(logic.currentAgentList[index]),
                    child: _buildAgentItem(logic.currentAgentList[index]))));
      }),
    );
  }

  Widget _buildTitle() {
    return Row(children: [
      Obx(() {
        var localColor = logic.currentTab.value == AgentLogic.TAB_LOCAL ? Colors.black : Colors.grey;
        var cloudColor = logic.currentTab.value == AgentLogic.TAB_CLOUD ? Colors.black : Colors.grey;
        return Row(children: [
          TextButton(
              onPressed: () => logic.switchTab(AgentLogic.TAB_LOCAL),
              child: Text('本地Agent管理', style: TextStyle(fontSize: 18, color: localColor))),
          TextButton(
              onPressed: () => logic.switchTab(AgentLogic.TAB_CLOUD),
              child: Text('云端Agent管理', style: TextStyle(fontSize: 18, color: cloudColor)))
        ]);
      }),
      const Spacer(),
      _buildRefreshButton(),
      const SizedBox(width: 10),
      _buildNewAgentButton(),
    ]);
  }

  Widget _buildSecondaryTitle() {
    return Obx(() {
      String tab = logic.currentSecondaryTab.value;
      var allColor = tab == AgentLogic.TAB_SEC_ALL ? Colors.black : Colors.grey;
      var sysColor = tab == AgentLogic.TAB_SEC_SYSTEM ? Colors.black : Colors.grey;
      var shareColor = tab == AgentLogic.TAB_SEC_SHARE ? Colors.black : Colors.grey;
      var meColor = tab == AgentLogic.TAB_SEC_MINE ? Colors.black : Colors.grey;
      return Offstage(
        offstage: logic.currentTab.value == AgentLogic.TAB_LOCAL,
        child: Container(
            margin: const EdgeInsets.only(top: 30),
            child: Row(children: [
              TextButton(
                  onPressed: () => logic.switchTab(AgentLogic.TAB_SEC_ALL),
                  child: Text('全部', style: TextStyle(fontSize: 16, color: allColor))),
              TextButton(
                  onPressed: () => logic.switchTab(AgentLogic.TAB_SEC_SYSTEM),
                  child: Text('系统', style: TextStyle(fontSize: 16, color: sysColor))),
              TextButton(
                  onPressed: () => logic.switchTab(AgentLogic.TAB_SEC_SHARE),
                  child: Text('分享', style: TextStyle(fontSize: 16, color: shareColor))),
              TextButton(
                  onPressed: () => logic.switchTab(AgentLogic.TAB_SEC_MINE),
                  child: Text('我的', style: TextStyle(fontSize: 16, color: meColor)))
            ])),
      );
    });
  }

  Widget _buildRefreshButton() {
    return InkWell(
      onTap: () => logic.onRefreshButtonClick(),
      child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 10), child: const Text('同步', style: TextStyle(color: Colors.black, fontSize: 16))),
    );
  }

  Widget _buildNewAgentButton() {
    return TextButton(
        style: ButtonStyle(
            padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(24, 18, 24, 18)),
            backgroundColor: WidgetStateProperty.all(buttonColor),
            shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)))),
        onPressed: () => logic.showCreateAgentDialog(),
        child: const Text('新建本地Agent', style: TextStyle(color: Colors.white, fontSize: 14)));
  }

  Widget _buildAgentItem(AgentDTO agent) {
    var iconPath = agent.icon ?? "";
    return Container(
      margin: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        border: Border.all(color: itemBorderColor),
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Column(
        children: [
          Container(
              margin: const EdgeInsets.fromLTRB(15, 10, 15, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      if (iconPath.isEmpty)
                        Container(
                            width: 30, height: 30, decoration: BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.circular(6)))
                      else
                        Image(image: FileImage(File(iconPath)), height: 30, width: 30, fit: BoxFit.cover),
                      const SizedBox(width: 16),
                      Expanded(
                          child: Text(agent.name ?? "",
                              style: const TextStyle(fontSize: 16, color: Colors.black), maxLines: 1, overflow: TextOverflow.ellipsis)),
                      Offstage(
                        offstage: !(agent.shareTip ?? false),
                        child: Container(
                          width: 44,
                          height: 24,
                          margin: const EdgeInsets.only(left: 4),
                          decoration: BoxDecoration(color: const Color.fromRGBO(255, 195, 0, 1), borderRadius: BorderRadius.circular(8.0)),
                          child: const Center(child: Text("已分享", style: TextStyle(fontSize: 10, color: Colors.white))),
                        ),
                      )
                    ],
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                      height: 64,
                      child: Text(agent.description ?? "",
                          maxLines: 3, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14))),
                ],
              )),
          const Spacer(),
          Obx(() {
            var isLocal = logic.currentTab.value == AgentLogic.TAB_LOCAL;
            double padding = isLocal ? 12 : 22;
            return Container(
              margin: EdgeInsets.symmetric(horizontal: padding),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  InkWell(
                    onTap: () {
                      logic.startChat(agent);
                    },
                    child: Row(children: [
                      Container(margin: const EdgeInsets.only(right: 4), child: const Icon(Icons.chat, color: Colors.blue, size: 16)),
                      const Text('聊天', style: TextStyle(color: Colors.blue, fontSize: 14))
                    ]),
                  ),
                  InkWell(
                    onTap: () {
                      if (isLocal) {
                        logic.jumpToAdjustPage(agent);
                      } else {
                        WebUtil.openAgentAdjustUrl(agent.id);
                      }
                    },
                    child: const Row(
                      children: [
                        Icon(Icons.newspaper, color: Colors.blue, size: 16),
                        SizedBox(width: 4),
                        Text('调试', style: TextStyle(color: Colors.blue, fontSize: 14))
                      ],
                    ),
                  ),
                  if (isLocal)
                    DropdownButtonHideUnderline(
                        child: DropdownButton2(
                            customButton: const Row(
                              children: [
                                Icon(Icons.more, color: Colors.blue, size: 16),
                                SizedBox(width: 4),
                                Text('更多', style: TextStyle(color: Colors.blue, fontSize: 14)),
                              ],
                            ),
                            dropdownStyleData: const DropdownStyleData(
                                width: 80,
                                offset: Offset(0, -10),
                                padding: EdgeInsets.symmetric(vertical: 0),
                                decoration: BoxDecoration(color: Colors.white)),
                            menuItemStyleData: const MenuItemStyleData(
                              height: 40,
                            ),
                            items: const [
                              DropdownMenuItem<String>(
                                value: "delete",
                                child: Center(child: Text("删除", style: TextStyle(fontSize: 14))),
                              )
                            ],
                            onChanged: (value) {
                              if (value == "delete") {
                                logic.removeAgent(agent.id);
                              }
                            }))
                ],
              ),
            );
          }),
          const SizedBox(height: 10)
        ],
      ),
    );
  }
}
