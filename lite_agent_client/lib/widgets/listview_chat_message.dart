import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/message.dart';

import '../utils/alarm_util.dart';
import '../utils/web_util.dart';
import 'chat_message_items.dart';
import 'common_widget.dart';

class ChatMessageListViewController extends GetxController {
  final ScrollController scrollController = ScrollController();
  final chatMessageList = <ChatMessageModel>[].obs;
  String? userAvatarPath;
  String? agentAvatarPath;
  var messageHoverItemId = "".obs;
  var showAudioButton = false;
  var activeAudioMessageId = "";
  Function(ChatMessageModel chatMessage)? onMessageThoughButtonClick;
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

  ChatMessageListView({super.key, required this.controller, required List<ChatMessageModel> chatMessageList}) {
    controller.chatMessageList.value = chatMessageList;
  }

  @override
  Widget build(BuildContext context) {
    return Obx(() => ListView.builder(
        controller: controller.scrollController,
        itemCount: controller.chatMessageList.length,
        itemBuilder: (context, index) => buildMessageItem(index, controller.chatMessageList[index])));
  }

  Widget buildMessageItem(int index, ChatMessageModel chatMessage) {
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

  Widget buildUserMessageItem(ChatMessageModel chatMessage, int index) {
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

  bool hasThought(ChatMessageModel chatMessage) {
    var messageList = chatMessage.subMessages ?? [];
    for (var message in messageList) {
      //chunk mode reasoning could be empty
      if (message.sendRole == ChatRoleType.Reasoning && message.message.trim().isEmpty) {
        continue;
      } else {
        return true;
      }
    }
    return false;
  }

  Widget buildAgentMessageItem(ChatMessageModel chatMessage, int index) {
    String message = chatMessage.message;
    if (message.trim().isEmpty) {
      message = chatMessage.isLoading ? "正在生成..." : "无返回结果";
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
                if (hasThought(chatMessage)) buildThoughtButton(chatMessage),
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

  Widget buildThoughtButton(ChatMessageModel chatMessage) {
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
