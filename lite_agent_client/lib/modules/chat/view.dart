import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/input_box_container.dart';

import '../../models/local_data_model.dart';
import '../../widgets/common_widget.dart';
import '../../widgets/listview_chat_message.dart';
import 'logic.dart';

class ChatPage extends StatelessWidget {
  ChatPage({Key? key}) : super(key: key);

  final bgColor = const Color(0xFFf5f5f5);
  final buttonColor = const Color(0xFF2a82f5);
  final chatBorderColor = const Color(0xFFd9d9d9);

  final logic = Get.put(ChatLogic());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        backgroundColor: Colors.white,
        body: Center(
            child: Row(children: [
          Container(
              width: 200,
              color: bgColor,
              child: Column(children: [
                const SizedBox(height: 20),
                Center(child: _buildChatButton()),
                const SizedBox(height: 20),
                Expanded(child: _buildAgentListView())
              ])),
          Obx(() => logic.currentConversation.value == null ? Container() : _buildChatView())
        ])));
  }

  Widget _buildChatButton() {
    return TextButton(
        style: ButtonStyle(
            padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 40)),
            backgroundColor: WidgetStateProperty.all(buttonColor),
            shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)))),
        onPressed: () => logic.onStartChatButtonClick(),
        child: const Text('开始聊天', style: TextStyle(color: Colors.white, fontSize: 14)));
  }

  Widget _buildAgentListView() {
    return Obx(() => ListView.builder(
        itemCount: logic.conversationList.length,
        itemBuilder: (context, index) {
          var conversation = logic.conversationList[index];
          var conversationId = conversation.agentId;
          var title = conversation.agent?.name ?? "";
          var isSelected = conversationId == logic.currentConversation.value?.agentId;
          var textColor = isSelected ? const Color(0xff474747) : const Color(0xff7b7b7b);
          return MouseRegion(
              onEnter: (event) => logic.conversationItemHoverId.value = index.toString(),
              onExit: (event) => logic.conversationItemHoverId.value = "",
              child: InkWell(
                onTap: () => logic.switchChatView(conversationId, true),
                child: Container(
                    padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 20),
                    child: Row(
                      children: [
                        buildAssetImage("icon_dashboard.png", 16, null),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(fontSize: 14, color: textColor, fontWeight: isSelected ? FontWeight.bold : FontWeight.normal),
                          ),
                        ),
                        Obx(() => Offstage(
                              offstage: logic.conversationItemHoverId.value != index.toString(),
                              child: DropdownButtonHideUnderline(
                                  child: DropdownButton2(
                                      customButton: Container(
                                        margin: const EdgeInsets.symmetric(horizontal: 4),
                                        child: buildAssetImage("icon_ellipsis.png", 18, textColor),
                                      ),
                                      dropdownStyleData: const DropdownStyleData(
                                          width: 60,
                                          offset: Offset(-10, 0),
                                          padding: EdgeInsets.symmetric(vertical: 0),
                                          decoration: BoxDecoration(color: Colors.white)),
                                      menuItemStyleData: const MenuItemStyleData(height: 30),
                                      items: const [
                                        DropdownMenuItem<String>(
                                          value: "delete",
                                          child: Center(child: Text("删除", style: TextStyle(fontSize: 12))),
                                        )
                                      ],
                                      onChanged: (value) {
                                        if (value == "delete") {
                                          logic.deleteConversation(conversationId);
                                        }
                                      })),
                            )),
                        Offstage(
                            offstage: !(conversation.isCloud),
                            child: Container(
                              margin: const EdgeInsets.symmetric(horizontal: 4),
                              child: buildAssetImage("icon_cloud.png", 16, textColor),
                            ))
                      ],
                    )),
              ));
        }));
  }

  Expanded _buildChatView() {
    AgentConversationBean? conversation = logic.currentConversation.value;
    AgentBean? agent = conversation?.agent;
    return Expanded(
        child: Column(children: [
      Container(
          padding: const EdgeInsets.fromLTRB(33, 12, 0, 12),
          child: Row(children: [
            Text(agent?.name ?? "", style: const TextStyle(fontSize: 18)),
            const Spacer(),
            Container(
              margin: const EdgeInsets.only(right: 12),
              child: InkWell(
                hoverColor: Colors.transparent,
                onTap: () => logic.showAgentInfo(),
                child: buildAssetImage("icon_ellipsis.png", 24, null),
              ),
            )
          ])),
      const Divider(height: 0.1),
      Expanded(
          child: Stack(children: [
        GestureDetector(
            onTap: () => logic.closeAgentInfo(),
            child: Row(
              children: [
                Expanded(
                    child: Column(
                  children: [
                    Expanded(
                      child: Container(
                          margin: const EdgeInsets.only(top: 10),
                          padding: const EdgeInsets.symmetric(vertical: 20),
                          child: ChatMessageListView(
                            controller: logic.listViewController,
                            chatMessageList: conversation?.chatMessageList ?? [],
                          )),
                    ),
                    Obx(() => Container(
                          padding: EdgeInsets.symmetric(horizontal: (logic.showThoughtProcessDetail.value) ? 0 : 40),
                          child: InputBoxContainer(controller: logic.inputBoxController),
                        ))
                  ],
                )),
                Obx(() => Offstage(
                      offstage: !logic.showThoughtProcessDetail.value,
                      child: buildThoughtDetailRow(),
                    ))
              ],
            )),
        Offstage(
          offstage: !logic.isShowDrawer.value,
          child: _buildAgentInfoRow(agent),
        )
      ]))
    ]));
  }

  Row _buildAgentInfoRow(AgentBean? agent) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        const VerticalDivider(width: 0.5, color: Colors.grey),
        Container(
          width: 360,
          color: Colors.white,
          child: Column(
            children: [
              const SizedBox(height: 80),
              SizedBox(width: 120, height: 120, child: buildAgentProfileImage(agent?.iconPath ?? "")),
              const SizedBox(height: 40),
              Text(agent?.name ?? "", style: const TextStyle(fontSize: 18, color: Colors.black)),
              const SizedBox(height: 20),
              SizedBox(
                width: 240,
                child: Center(child: Text(agent?.description ?? "", maxLines: 5, style: const TextStyle(fontSize: 14, color: Colors.grey))),
              ),
              const Spacer(),
              InkWell(
                  onTap: () => logic.clearAllMessage(),
                  child: Container(
                      width: 200,
                      height: 40,
                      decoration: const BoxDecoration(
                        color: Color(0xFF2a82f5),
                        borderRadius: BorderRadius.all(Radius.circular(4)),
                      ),
                      child: const Center(child: Text('清空上下文', style: TextStyle(color: Colors.white, fontSize: 14))))),
              const SizedBox(height: 20),
              InkWell(
                  onTap: () => logic.jumpToAdjustPage(),
                  child: Container(
                      width: 200,
                      height: 40,
                      decoration: BoxDecoration(
                          border: Border.all(color: const Color.fromRGBO(196, 196, 196, 1)),
                          borderRadius: const BorderRadius.all(Radius.circular(4))),
                      child: const Center(child: Text('进入调试', style: TextStyle(color: Color.fromRGBO(196, 196, 196, 1), fontSize: 14))))),
              const SizedBox(height: 80),
            ],
          ),
        ),
      ],
    );
  }

  Row buildThoughtDetailRow() {
    return Row(
      children: [
        verticalLine(),
        SizedBox(
          width: 300,
          child: Column(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                child: Row(
                  children: [
                    const Text("过程详情", style: TextStyle(fontSize: 16, color: Color(0xff333333))),
                    const Spacer(),
                    InkWell(
                      onTap: () => logic.showThoughtProcessDetail.value = false,
                      child: buildAssetImage("icon_close.png", 16, null),
                    )
                  ],
                ),
              ),
              horizontalLine(),
              Expanded(
                child: Obx(() => ListView.builder(
                    itemCount: logic.currentSubMessageList.length,
                    itemBuilder: (context, index) => Container(
                          margin: const EdgeInsets.all(10),
                          padding: const EdgeInsets.symmetric(horizontal: 6),
                          child: buildSubMessageItem(logic.currentSubMessageList[index], () => logic.currentSubMessageList.refresh()),
                        ))),
              ),
            ],
          ),
        )
      ],
    );
  }
}
