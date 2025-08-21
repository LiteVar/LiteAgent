import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

import '../models/dto/retrieval_result.dart';
import '../utils/alarm_util.dart';
import '../utils/web_util.dart';
import 'common_widget.dart';

class ChatMessageListViewController extends GetxController {
  final ScrollController scrollController = ScrollController();
  final chatMessageList = <ChatMessage>[].obs;
  String? userAvatarPath;
  String? agentAvatarPath;
  var messageHoverItemId = "".obs;
  var showAudioButton = false;
  var activeAudioMessageId = "";
  Function(ChatMessage chatMessage)? onMessageThoughButtonClick;
  Function(int index, String message)? onMessageAudioButtonClick;

  ChatMessageListViewController();

  void scrollListToBottom({animate = true}) {
    if (!scrollController.hasClients) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (animate) {
        scrollController.animateTo(scrollController.position.maxScrollExtent,
            duration: const Duration(milliseconds: 200), curve: Curves.easeOutQuart);
      } else {
        scrollController.jumpTo(scrollController.position.maxScrollExtent);
      }
    });
  }

  void copyToClipboard(String string) {
    Clipboard.setData(ClipboardData(text: string)).then((text) => AlarmUtil.showAlertToast("复制成功"));
  }

  void setAudioButtonVisible(bool visible) {
    showAudioButton = visible;
  }

  void refreshButton() {
    messageHoverItemId.refresh();
  }

  @override
  void dispose() {
    scrollController.dispose();
    onMessageThoughButtonClick = null;
    onMessageAudioButtonClick = null;

    super.dispose();
  }
}

class ChatMessageListView extends StatelessWidget {
  final ChatMessageListViewController controller;

  ChatMessageListView({super.key, required this.controller, required List<ChatMessage> chatMessageList}) {
    controller.chatMessageList.value = chatMessageList;
  }

  @override
  Widget build(BuildContext context) {
    return Obx(() => ListView.builder(
        controller: controller.scrollController,
        itemCount: controller.chatMessageList.length,
        itemBuilder: (context, index) => buildMessageItem(index, controller.chatMessageList[index])));
  }

  Widget buildMessageItem(int index, ChatMessage chatMessage) {
    if (chatMessage.sendRole == ChatRoleType.User) {
      return MouseRegion(
        onEnter: (event) => controller.messageHoverItemId.value = index.toString(),
        onExit: (event) => controller.messageHoverItemId.value = "",
        child: buildUserMessageItem(chatMessage, index),
      );
    } else if (chatMessage.sendRole == ChatRoleType.Agent) {
      return MouseRegion(
        onEnter: (event) => controller.messageHoverItemId.value = index.toString(),
        onExit: (event) => controller.messageHoverItemId.value = "",
        child: buildAgentMessageItem(chatMessage, index),
      );
    } else {
      return Container();
    }
  }

  Widget buildUserMessageItem(ChatMessage chatMessage, int index) {
    return Container(
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
                    visible: controller.messageHoverItemId.value == index.toString(),
                    maintainSize: true,
                    maintainAnimation: true,
                    maintainState: true,
                    child: InkWell(
                        onTap: () => controller.copyToClipboard(chatMessage.message),
                        child: Container(
                            margin: const EdgeInsets.symmetric(vertical: 10),
                            child: buildAssetImage("icon_copy.png", 16, const Color(0xff999999))))))
              ],
            ),
          ),
          Container(
            width: 25,
            height: 25,
            margin: const EdgeInsets.only(left: 10, top: 4),
            child: buildUserProfileImage(controller.userAvatarPath ?? ""),
          )
        ],
      ),
    );
  }

  Widget buildAgentMessageItem(ChatMessage chatMessage, int index) {
    String message = chatMessage.isLoading ? "正在生成..." : chatMessage.message;
    if (message.isEmpty) {
      message = "无返回结果";
    }

    return Container(
      margin: const EdgeInsets.only(top: 10),
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
              width: 25,
              height: 25,
              margin: const EdgeInsets.only(right: 10, top: 4),
              child: buildAgentProfileImage(controller.agentAvatarPath ?? "")),
          Flexible(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if ((chatMessage.subMessages?.length ?? 0) > 0) buildThoughtButton(chatMessage),
                Container(
                  padding: const EdgeInsets.only(top: 10),
                  child: MarkdownBody(
                      data: message,
                      onTapLink: (text, url, title) async {
                        if (url != null) {
                          WebUtil.openUrl(url);
                        }
                      }),
                ),
                Obx(() => Visibility(
                    visible: controller.messageHoverItemId.value == index.toString(),
                    maintainSize: true,
                    maintainAnimation: true,
                    maintainState: true,
                    child: Row(
                      children: [
                        InkWell(
                            onTap: () => controller.copyToClipboard(chatMessage.message),
                            child: Container(
                                margin: const EdgeInsets.symmetric(vertical: 10),
                                child: buildAssetImage("icon_copy.png", 16, const Color(0xff999999)))),
                        const SizedBox(width: 10),
                        Offstage(
                          offstage: chatMessage.isLoading || !controller.showAudioButton,
                          child: InkWell(
                              onTap: () => controller.onMessageAudioButtonClick?.call(index, chatMessage.message),
                              child: Container(
                                  child: buildAssetImage(
                                      controller.activeAudioMessageId == index.toString() ? "icon_audio_stop.png" : "icon_audio_play.png",
                                      24,
                                      const Color(0xff999999)))),
                        ),
                      ],
                    )))
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget buildThoughtButton(ChatMessage chatMessage) {
    return InkWell(
        onTap: () => controller.onMessageThoughButtonClick?.call(chatMessage),
        child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(border: Border.all(color: const Color(0xff2A82E4)), borderRadius: BorderRadius.circular(8)),
            margin: const EdgeInsets.only(left: 5),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text("过程详情", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                if (chatMessage.isLoading)
                  Container(
                    margin: const EdgeInsets.only(left: 6),
                    width: 12,
                    height: 12,
                    child: const CircularProgressIndicator(strokeWidth: 2, valueColor: AlwaysStoppedAnimation<Color>(Color(0xff2A82E4))),
                  ),
              ],
            )));
  }
}

Widget buildSubMessageItem(ChatMessage chatMessage, Function refresh) {
  if (chatMessage.sendRole == ChatRoleType.Tool) {
    var receivedMessage = chatMessage.receivedMessage ?? "";
    receivedMessage = receivedMessage.isEmpty && chatMessage.isLoading ? "正在响应中..." : receivedMessage;
    if (receivedMessage.isEmpty) {
      receivedMessage = "无返回结果";
    }
    var isLibraryTool = chatMessage.roleName == "GET-retrieve";
    if (isLibraryTool) {
      String retrieveContent = "";
      try {
        Map<String, dynamic> retrieveJson = jsonDecode(chatMessage.message);
        retrieveContent = retrieveJson["query"];
      } catch (e) {
        retrieveContent = chatMessage.message;
      }

      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("检索知识库", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
          const SizedBox(height: 10),
          Text("检索内容:$retrieveContent", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
          const SizedBox(height: 10),
          Text("检索结果:$receivedMessage", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
        ],
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text("调用${chatMessage.roleName}工具", style: const TextStyle(fontSize: 14, color: Color(0xff666666))),
        const SizedBox(height: 10),
        Text("接收信息:${chatMessage.message}", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
        const SizedBox(height: 10),
        Text("工具结果:$receivedMessage", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
      ],
    );
  } else if (chatMessage.sendRole == ChatRoleType.SubAgent) {
    var receivedMessage = chatMessage.receivedMessage ?? "";
    receivedMessage = receivedMessage.isEmpty && chatMessage.isLoading ? "正在生成..." : receivedMessage;
    if (receivedMessage.isEmpty) {
      receivedMessage = "无返回结果";
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text("调用Agent:${chatMessage.roleName}", style: const TextStyle(fontSize: 14, color: Color(0xff666666))),
            InkWell(
                onTap: () {
                  chatMessage.isMessageExpanded = !chatMessage.isMessageExpanded;
                  refresh();
                },
                child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 4),
                    child: buildAssetImage(chatMessage.isMessageExpanded ? "icon_up.png" : "icon_down.png", 12, Colors.black))),
          ],
        ),
        Offstage(
          offstage: !chatMessage.isMessageExpanded,
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const SizedBox(height: 10),
            Text("输入指令:${chatMessage.message}", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
            const SizedBox(height: 10),
            Text("输出内容:$receivedMessage", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
            if (chatMessage.subMessages != null)
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const SizedBox(height: 10),
                ...List.generate(
                  chatMessage.subMessages!.length,
                  (index) => buildSubMessageItem(chatMessage.subMessages![index], refresh),
                )
              ])
          ]),
        ),
      ],
    );
  } else if (chatMessage.sendRole == ChatRoleType.Reflection) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("反思阶段", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
        const SizedBox(height: 10),
        Text("反思内容:${chatMessage.message}", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
        const SizedBox(height: 10),
        Text("反思结果:${chatMessage.receivedMessage ?? ""}", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
      ],
    );
  } else if (chatMessage.sendRole == ChatRoleType.Reasoning) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("思考过程", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
        const SizedBox(height: 10),
        Text(chatMessage.message, style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
      ],
    );
  } else {
    return Container(); // Handle unexpected type
  }
}
