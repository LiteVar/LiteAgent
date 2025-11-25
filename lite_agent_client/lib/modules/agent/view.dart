import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';
import 'package:lite_agent_client/utils/web_util.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import 'logic.dart';

class AgentPage extends StatelessWidget {
  AgentPage({Key? key}) : super(key: key);

  final logic = Get.put(AgentLogic());

  final buttonColor = const Color(0xFF2a82f5);
  final itemBorderColor = const Color(0xFFd9d9d9);
  final itemSpacingWidth = 20.0;

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
        if (logic.currentAgentList.isNotEmpty) {
          return Container(
            margin: const EdgeInsets.all(15),
            child: LayoutBuilder(
              builder: (context, constraints) {
                return ScrollConfiguration(
                    behavior: ScrollConfiguration.of(context).copyWith(scrollbars: false),
                    child: SingleChildScrollView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        child: Align(
                          alignment: Alignment.centerLeft,
                          child: Wrap(
                              spacing: itemSpacingWidth,
                              runSpacing: itemSpacingWidth,
                              children: List.generate(
                                  logic.currentAgentList.length,
                                  (index) => InkWell(
                                      onTap: () => logic.showAgentDetailDialog(logic.currentAgentList[index]),
                                      child: _buildAgentItem(constraints.maxWidth, logic.currentAgentList[index])))))));
              },
            ),
          );
        } else {
          String text = logic.currentTab.value == AgentLogic.TAB_LOCAL ? "暂无Agent，请创建" : "暂无Agent";
          return Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              SizedBox(height: 330, width: 400, child: Image.asset('assets/images/icon_list_empty.png', fit: BoxFit.contain)),
              Text(text, style: const TextStyle(fontSize: 14, color: Colors.grey)),
              const SizedBox(height: 40)
            ],
          );
        }
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
      Obx(() => Offstage(offstage: logic.currentTab.value == AgentLogic.TAB_LOCAL, child: _buildRefreshButton())),
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
    return DropdownButtonHideUnderline(
      child: DropdownButton2<String>(
        customButton: TextButton(
          style: ButtonStyle(
              padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(24, 18, 16, 18)),
              backgroundColor: WidgetStateProperty.all(buttonColor),
              shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)))),
          onPressed: null,
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('新建本地Agent', style: TextStyle(color: Colors.white, fontSize: 14)),
              SizedBox(width: 8),
              Icon(Icons.keyboard_arrow_down, color: Colors.white, size: 16),
            ],
          ),
        ),
        dropdownStyleData: const DropdownStyleData(
          width: null,
          offset: Offset(0, -5),
          padding: EdgeInsets.symmetric(vertical: 6),
          decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.all(Radius.circular(8)),
              boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 8, offset: Offset(0, 4))]),
        ),
        menuItemStyleData: const MenuItemStyleData(height: 40, padding: EdgeInsets.zero),
        items: const [
          DropdownMenuItem<String>(
              value: "create", child: Center(child: Text("新建本地Agent", style: TextStyle(fontSize: 14, color: Colors.black)))),
          DropdownMenuItem<String>(
              value: "import", child: Center(child: Text("导入Agent", style: TextStyle(fontSize: 14, color: Colors.black)))),
        ],
        onChanged: (value) {
          if (value == "create") {
            logic.showCreateAgentDialog();
          } else if (value == "import") {
            logic.showImportAgentDialog();
          }
        },
      ),
    );
  }

  Widget _buildAgentItem(double maxWidth, AgentModel agent) {
    // 计算子项宽度（减去间距）
    final itemWidth = (maxWidth - itemSpacingWidth * 3) / 4;
    var iconPath = agent.iconPath;
    return Container(
      width: itemWidth,
      decoration: BoxDecoration(
        border: Border.all(color: itemBorderColor),
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
              margin: const EdgeInsets.fromLTRB(15, 10, 15, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      SizedBox(width: 30, height: 30, child: buildAgentProfileImage(iconPath)),
                      const SizedBox(width: 16),
                      Expanded(
                          child: Text(agent.name,
                              style: const TextStyle(fontSize: 16, color: Colors.black), maxLines: 1, overflow: TextOverflow.ellipsis)),
                      Offstage(
                        offstage: !agent.shareFlag,
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
                    height: 84, // 固定高度区域
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (agent.autoAgentFlag == true)
                          const Padding(
                            padding: EdgeInsets.only(bottom: 4),
                            child: Text("类型:Auto Multi Agent", style: TextStyle(fontSize: 14)),
                          ),
                        Text(agent.description, maxLines: 3, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14)),
                      ],
                    ),
                  ),
                ],
              )),
          const SizedBox(height: 10),
          Obx(() {
            var isLocal = logic.currentTab.value == AgentLogic.TAB_LOCAL;
            double padding = isLocal ? 12 : 22;
            return Container(
              margin: EdgeInsets.symmetric(horizontal: padding),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  InkWell(
                    onTap: () => logic.startChat(agent),
                    child: Row(children: [
                      Container(margin: const EdgeInsets.only(right: 4), child: buildAssetImage("icon_message.png", 16, Colors.blue)),
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
                    }.throttle(),
                    child: Row(
                      children: [
                        Container(margin: const EdgeInsets.only(right: 4), child: buildAssetImage("icon_file_text.png", 16, Colors.blue)),
                        const Text('调试', style: TextStyle(color: Colors.blue, fontSize: 14))
                      ],
                    ),
                  ),
                  if (isLocal && agent.autoAgentFlag != true)
                    DropdownButtonHideUnderline(
                        child: DropdownButton2(
                            customButton: Row(
                              children: [
                                buildAssetImage("icon_menu.png", 16, Colors.blue),
                                const SizedBox(width: 4),
                                const Text('更多', style: TextStyle(color: Colors.blue, fontSize: 14)),
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
                                logic.showRemoveAgentDialog(agent.id);
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
