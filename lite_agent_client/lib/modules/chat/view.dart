import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:get/get.dart';

import '../../models/local_data_model.dart';
import '../../utils/web_util.dart';
import '../../widgets/common_widget.dart';
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
            shape: WidgetStateProperty.all<RoundedRectangleBorder>(
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
            )),
        onPressed: () => logic.onStartChatButtonClick(),
        child: const Text('开始聊天', style: TextStyle(color: Colors.white, fontSize: 14)));
  }

  Widget _buildAgentListView() {
    return Obx(() {
      return ListView.builder(
          itemCount: logic.conversationList.length,
          itemBuilder: (context, index) {
            var conversation = logic.conversationList[index];
            var conversationId = conversation.agentId;
            var title = conversation.agent?.name ?? "";
            return InkWell(
              onTap: () => logic.switchChatView(conversationId, true),
              child: Container(
                  padding: const EdgeInsets.fromLTRB(20, 10, 20, 10),
                  child: Row(
                    children: [
                      buildAssetImage("icon_dashboard.png", 16, null),
                      const SizedBox(width: 10),
                      Expanded(child: Text(title, style: const TextStyle(fontSize: 14, color: Color.fromRGBO(71, 71, 71, 1)))),
                      Offstage(
                          offstage: !(conversation.isCloud),
                          child: Container(
                            margin: const EdgeInsets.symmetric(horizontal: 4),
                            child: buildAssetImage("icon_cloud.png", 16, Colors.grey),
                          ))
                    ],
                  )),
            );
          });
    });
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
            child: Column(children: [
              Expanded(
                child: Container(
                    padding: const EdgeInsets.fromLTRB(0, 30, 0, 0),
                    child: ScrollConfiguration(
                        behavior: ScrollConfiguration.of(Get.context!).copyWith(scrollbars: true),
                        child: ListView.builder(
                            controller: logic.chatScrollController,
                            itemCount: conversation?.chatMessageList.length ?? 0,
                            itemBuilder: (context, index) {
                              var chatMessage = conversation?.chatMessageList[index];
                              return chatMessage != null ? _buildMessageItem(index, chatMessage) : Container();
                            }))),
              ),
              _buildInputView()
            ])),
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

  Widget _buildMessageItem(int index, ChatMessage chatMessage) {
    if (chatMessage.sendRole == ChatRole.User) {
      var iconPath = logic.account?.avatar ?? "";
      return MouseRegion(
          onEnter: (event) => logic.messageHoverItemId.value = index.toString(),
          onExit: (event) => logic.messageHoverItemId.value = "",
          child: Container(
            margin: const EdgeInsets.only(top: 10),
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Flexible(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(8))),
                        child: MarkdownBody(
                            data: chatMessage.message,
                            onTapLink: (text, url, title) async {
                              if (url != null) {
                                WebUtil.openUrl(url);
                              }
                            }),
                      ),
                      Obx(() => Visibility(
                          visible: logic.messageHoverItemId.value == index.toString(),
                          maintainSize: true,
                          maintainAnimation: true,
                          maintainState: true,
                          child: InkWell(
                              onTap: () => logic.copyToClipboard(chatMessage.message),
                              child: Container(
                                  margin: const EdgeInsets.symmetric(vertical: 10),
                                  child: buildAssetImage("icon_copy.png", 16, const Color(0xff999999))))))
                    ],
                  ),
                ),
                Container(width: 25, height: 25, margin: const EdgeInsets.only(left: 10, top: 4), child: buildUserProfileImage(iconPath))
              ],
            ),
          ));
    } else if (chatMessage.sendRole == ChatRole.Agent) {
      var agent = logic.currentConversation.value?.agent;
      String iconPath = agent?.iconPath ?? "";
      String message = chatMessage.isLoading ? "正在生成..." : chatMessage.message;
      return MouseRegion(
          onEnter: (event) => logic.messageHoverItemId.value = index.toString(),
          onExit: (event) => logic.messageHoverItemId.value = "",
          child: Container(
            margin: const EdgeInsets.only(top: 10),
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(width: 25, height: 25, margin: const EdgeInsets.only(right: 10, top: 4), child: buildAgentProfileImage(iconPath)),
                Flexible(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if ((chatMessage.thoughtList?.length ?? 0) > 0) buildThoughtProcessColumn(chatMessage),
                      Container(
                        padding: const EdgeInsets.only(top: 10),
                        //decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(8))),
                        child: MarkdownBody(
                            data: message,
                            onTapLink: (text, url, title) async {
                              if (url != null) {
                                WebUtil.openUrl(url);
                              }
                            }),
                      ),
                      if ((chatMessage.childAgentMessageList?.length ?? 0) > 0)
                        ...List.generate(
                          chatMessage.childAgentMessageList?.length ?? 0,
                          (index) => Container(
                              margin: const EdgeInsets.only(top: 5),
                              child: Text(chatMessage.childAgentMessageList![index],
                                  style: const TextStyle(fontSize: 12, color: Color(0xff999999)))),
                        ),
                      Obx(() => Visibility(
                          visible: logic.messageHoverItemId.value == index.toString(),
                          maintainSize: true,
                          maintainAnimation: true,
                          maintainState: true,
                          child: InkWell(
                              onTap: () => logic.copyToClipboard(chatMessage.message),
                              child: Container(
                                  margin: const EdgeInsets.symmetric(vertical: 10),
                                  child: buildAssetImage("icon_copy.png", 16, const Color(0xff999999))))))
                    ],
                  ),
                ),
              ],
            ),
          ));
    } else {
      return Container();
    }
  }

  Widget _buildInputView() {
    return Container(
        width: 600,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        margin: const EdgeInsets.symmetric(vertical: 20),
        decoration: BoxDecoration(
          border: Border.all(color: chatBorderColor),
          borderRadius: BorderRadius.circular(7.0),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Obx(() {
              var isShow = logic.selectImagePath.value.isNotEmpty;
              return isShow
                  ? Container(
                      width: 80,
                      height: 80,
                      margin: const EdgeInsets.fromLTRB(15, 10, 0, 10),
                      color: Colors.grey,
                      child: Stack(fit: StackFit.loose, children: [
                        SizedBox(
                          width: 80,
                          height: 80,
                          child: Image(image: FileImage(File(logic.selectImagePath.value)), fit: BoxFit.cover),
                        ),
                        InkWell(
                          onTap: () {
                            logic.selectImagePath.value = "";
                          },
                          child: const Row(children: [Spacer(), Icon(Icons.close, size: 16, color: Colors.white)]),
                        )
                      ]))
                  : Container();
            }),
            SizedBox(
              height: 40,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  /*InkWell(
                      hoverColor: Colors.transparent,
                      splashColor: Colors.transparent,
                      onTap: () {
                        logic.selectImageFile();
                      },
                      child: Icon(Icons.add, size: 24)),*/
                  Expanded(
                      child: TextField(
                          enabled: logic.enableInput.value,
                          focusNode: logic.chatFocusNode,
                          autofocus: true,
                          onSubmitted: (string) {
                            logic.onChatButtonPress();
                          },
                          controller: logic.chatController,
                          decoration: InputDecoration(
                              hintText: logic.enableInput.value ? '请输入聊天内容' : '反思Agent不能进行聊天对话',
                              border: InputBorder.none,
                              isDense: true,
                              contentPadding: const EdgeInsets.symmetric(horizontal: 4)),
                          style: const TextStyle(fontSize: 14))),
                  InkWell(
                      hoverColor: Colors.transparent,
                      splashColor: Colors.transparent,
                      onTap: () {
                        logic.onChatButtonPress();
                      },
                      child: Container(
                          margin: const EdgeInsets.only(right: 4),
                          padding: const EdgeInsets.all(4),
                          child: buildAssetImage("icon_send.png", 20, const Color(0xffb3b3b3))))
                ],
              ),
            )
          ],
        ));
  }

  Column buildThoughtProcessColumn(ChatMessage chatMessage) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
            padding: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                InkWell(
                    onTap: () {
                      chatMessage.isThoughtExpanded = !chatMessage.isThoughtExpanded;
                      logic.currentConversation.refresh();
                    },
                    child: Row(children: [
                      const Text("思考过程", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
                      Container(
                          margin: const EdgeInsets.only(left: 5),
                          child:
                              buildAssetImage(chatMessage.isThoughtExpanded ? "icon_up.png" : "icon_down.png", 12, const Color(0xff333333)))
                    ])),
                const Spacer()
              ],
            )),
        Offstage(
          offstage: !chatMessage.isThoughtExpanded,
          child: Column(
            children: [
              ...List.generate(
                chatMessage.thoughtList?.length ?? 0,
                (index) => Container(margin: const EdgeInsets.only(top: 5), child: buildThoughtItem(chatMessage.thoughtList![index])),
              )
            ],
          ),
        )
      ],
    );
  }

  Widget buildThoughtItem(Thought thought) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("工具调用", style: TextStyle(fontSize: 14, color: Color(0xff999999))),
        const Text("接收信息:", style: TextStyle(fontSize: 12, color: Color(0xff999999))),
        Container(
            margin: const EdgeInsets.only(left: 20),
            child: Text(thought.sentMessage, style: const TextStyle(fontSize: 12, color: Color(0xff999999)))),
        Text("${thought.roleName}:", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
        Container(
            margin: const EdgeInsets.only(left: 20),
            child: Text(thought.receivedMessage, style: const TextStyle(fontSize: 12, color: Color(0xff999999)))),
      ],
    );
  }
}
