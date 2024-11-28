import 'dart:io';

import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../models/local_data_model.dart';
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
          Obx(() {
            if (logic.currentConversation.value == null) {
              return Container();
            } else {
              return _buildChatView();
            }
          })
        ])));
  }

  Widget _buildChatButton() {
    return TextButton(
        style: ButtonStyle(
            padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 40)),
            backgroundColor: WidgetStateProperty.all(buttonColor),
            shape: WidgetStateProperty.all<RoundedRectangleBorder>(
              RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(4.0),
              ),
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
              onTap: () {
                logic.switchChatView(conversationId);
              },
              child: Container(
                  padding: const EdgeInsets.fromLTRB(20, 10, 20, 10),
                  child: Row(
                    children: [
                      const Icon(Icons.history, size: 14),
                      const SizedBox(width: 10),
                      Expanded(child: Text(title, style: const TextStyle(fontSize: 14, color: Color.fromRGBO(71, 71, 71, 1)))),
                      Offstage(
                          offstage: !(conversation.isCloud ?? false),
                          child: Container(
                              margin: const EdgeInsets.symmetric(horizontal: 4),
                              child: const Icon(Icons.cloud, size: 14, color: Colors.grey)))
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
                  onTap: () {
                    logic.showAgentInfo();
                  },
                  child: const Icon(Icons.more, size: 24)),
            )
          ])),
      const Divider(height: 0.1),
      Expanded(
          child: Stack(children: [
        GestureDetector(
            onTap: () {
              logic.closeAgentInfo();
            },
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
                              if (chatMessage != null) {
                                return _buildMessageItem(chatMessage);
                              } else {
                                return Container();
                              }
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
              if (agent != null && agent.iconPath.isNotEmpty)
                Image(image: FileImage(File(agent.iconPath)), height: 120, width: 120, fit: BoxFit.cover)
              else
                Container(width: 120, height: 120, color: Colors.grey),
              const SizedBox(height: 40),
              Text(agent?.name ?? "", style: const TextStyle(fontSize: 18, color: Colors.black)),
              const SizedBox(height: 20),
              SizedBox(
                width: 240,
                child: Center(child: Text(agent?.description ?? "", maxLines: 5, style: const TextStyle(fontSize: 14, color: Colors.grey))),
              ),
              const Spacer(),
              InkWell(
                  onTap: () {
                    logic.clearAllMessage();
                  },
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
                  onTap: () {
                    logic.jumpToAdjustPage();
                  },
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

  Widget _buildMessageItem(ChatMessage chatMessage) {
    if (chatMessage.sendRole == ChatRole.User) {
      return buildUserMessageItem(chatMessage);
    } else if (chatMessage.sendRole == ChatRole.Agent) {
      return buildAgentMessageItem(chatMessage);
    } else {
      return Container();
    }
  }

  Container buildAgentMessageItem(ChatMessage chatMessage) {
    return Container(
      margin: const EdgeInsets.fromLTRB(50, 15, 50, 0),
      child: Row(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(children: [
                Container(
                  height: 25,
                  width: 25,
                  color: Colors.grey,
                  margin: const EdgeInsets.fromLTRB(0, 0, 5, 5),
                  child: const Icon(Icons.android, size: 25),
                ),
                Obx(() {
                  var isLogin = logic.account.value != null;
                  if (isLogin) {
                    var name = logic.currentConversation.value?.agent?.name ?? "Agent";
                    return Text(name, style: const TextStyle(fontSize: 14, color: Colors.black));
                  } else {
                    return Container();
                  }
                })
              ]),
              SizedBox(
                width: 300,
                child: Text(chatMessage.message, style: const TextStyle(fontSize: 14, color: Colors.black)),
              )
            ],
          )
        ],
      ),
    );
  }

  Container buildUserMessageItem(ChatMessage chatMessage) {
    return Container(
      margin: const EdgeInsets.fromLTRB(50, 15, 50, 0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Row(children: [
              Obx(() {
                var isLogin = logic.account.value != null;
                if (isLogin) {
                  String username = logic.account.value?.name ?? chatMessage.userName;
                  return Text(username, style: const TextStyle(fontSize: 14, color: Colors.black));
                } else {
                  return Container();
                }
              }),
              Container(
                  height: 25,
                  width: 25,
                  color: Colors.grey,
                  margin: const EdgeInsets.fromLTRB(5, 0, 0, 5),
                  child: const Icon(Icons.android, size: 25))
            ]),
            SizedBox(
              width: 300,
              child: Text(chatMessage.message, textAlign: TextAlign.right, style: const TextStyle(fontSize: 14, color: Colors.black)),
            ),
            if ((chatMessage.imgFilePath ?? "").isNotEmpty)
              Container(
                  margin: const EdgeInsets.only(top: 5),
                  width: 80,
                  height: 80,
                  color: Colors.grey,
                  child: Image(image: FileImage(File(chatMessage.imgFilePath ?? "")), fit: BoxFit.cover))
          ])
        ],
      ),
    );
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
                          focusNode: logic.chatFocusNode,
                          autofocus: true,
                          onSubmitted: (string) {
                            logic.onChatButtonPress();
                          },
                          controller: logic.chatController,
                          decoration: const InputDecoration(
                              hintText: '请输入聊天内容',
                              border: InputBorder.none,
                              isDense: true,
                              contentPadding: EdgeInsets.symmetric(horizontal: 4)),
                          style: const TextStyle(fontSize: 14))),
                  InkWell(
                      hoverColor: Colors.transparent,
                      splashColor: Colors.transparent,
                      onTap: () {
                        logic.onChatButtonPress();
                      },
                      child: const Icon(Icons.send, size: 24))
                ],
              ),
            )
          ],
        ));
  }
}
