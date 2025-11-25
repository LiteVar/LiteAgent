import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/retrieval_history.dart';
import 'package:lite_agent_client/models/local/message.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_retrieval_history.dart';

import 'common_widget.dart';

/// 聊天消息构建器
/// 包含各种类型消息的构建方法
class ChatMessageItem {
  /// 构建子消息项的主入口方法
  static Widget buildSubMessageItem(ChatMessageModel chatMessage, Function refresh) {
    switch (chatMessage.sendRole) {
      case ChatRoleType.Tool:
        return buildToolMessageItem(chatMessage, refresh);
      case ChatRoleType.SubAgent:
        return buildSubAgentMessageItem(chatMessage, refresh);
      case ChatRoleType.Reflection:
        return buildReflectionMessageItem(chatMessage);
      case ChatRoleType.Reasoning:
        return buildReasoningMessageItem(chatMessage);
      default:
        return Container(); // Handle unexpected type
    }
  }

  /// 构建工具消息项
  static Widget buildToolMessageItem(ChatMessageModel chatMessage, Function refresh) {
    var receivedMessage = chatMessage.receivedMessage ?? "";
    receivedMessage = receivedMessage.isEmpty && chatMessage.isLoading ? "正在响应中..." : receivedMessage;
    if (receivedMessage.isEmpty) {
      receivedMessage = "无返回结果";
    }

    // 检查是否为知识库检索工具
    var isLibraryTool = chatMessage.roleName == "GET-retrieveDesktop";
    if (isLibraryTool) {
      return buildLibraryToolMessageItem(chatMessage, receivedMessage);
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
  }

  /// 构建知识库检索工具消息项
  static Widget buildLibraryToolMessageItem(ChatMessageModel chatMessage, String receivedMessage) {
    String retrieveContent = "";
    List<RetrievalHistoryDto> retrievalResults = [];

    // 解析检索内容
    try {
      Map<String, dynamic> retrieveJson = jsonDecode(chatMessage.message);
      retrieveContent = retrieveJson["query"] ?? "";
    } catch (e) {
      retrieveContent = chatMessage.message;
    }

    // 解析检索结果
    try {
      Map<String, dynamic> responseJson = jsonDecode(receivedMessage);
      if (responseJson.containsKey("data") && responseJson["data"] is Map<String, dynamic>) {
        Map<String, dynamic> data = responseJson["data"];
        if (data.containsKey("history") && data["history"] is List) {
          List<dynamic> historyList = data["history"];
          for (var historyItem in historyList) {
            if (historyItem is Map<String, dynamic>) {
              try {
                RetrievalHistoryDto result = RetrievalHistoryDto.fromJson(historyItem);
                retrievalResults.add(result);
              } catch (e) {
                Log.e("Failed to parse history item: $e");
              }
            }
          }
          // 处理空结果情况
          if (retrievalResults.isEmpty) {
            receivedMessage = "无";
          }
        }
      }
    } catch (e) {
      Log.e("Failed to parse receivedMessage: $e");
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("检索知识库", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
        const SizedBox(height: 10),
        Text("检索内容:$retrieveContent", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
        const SizedBox(height: 10),
        if (retrievalResults.isNotEmpty) ...[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text("检索结果:", style: TextStyle(fontSize: 12, color: Color(0xff999999))),
              Expanded(
                child: Wrap(
                  children: retrievalResults.asMap().entries.map((entry) {
                    int index = entry.key;
                    RetrievalHistoryDto result = entry.value;
                    String displayText = result.datasetName;
                    return Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        InkWell(
                          onTap: () =>
                              Get.dialog(RetrievalHistoryDialog(historyId: result.id, title: retrieveContent), barrierDismissible: true),
                          child: Text(displayText, style: const TextStyle(fontSize: 12, color: Color(0xff2A82E4))),
                        ),
                        if (index < retrievalResults.length - 1) const Text(", ", style: TextStyle(fontSize: 12, color: Color(0xff999999))),
                      ],
                    );
                  }).toList(),
                ),
              ),
            ],
          ),
        ] else ...[
          Text("检索结果:$receivedMessage", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
        ],
      ],
    );
  }

  /// 构建子智能体消息项
  static Widget buildSubAgentMessageItem(ChatMessageModel chatMessage, Function refresh) {
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
            Flexible(
                child: Text("调用Agent:${chatMessage.roleName}",
                    style: const TextStyle(fontSize: 14, color: Color(0xff666666)), maxLines: 1, overflow: TextOverflow.ellipsis)),
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
  }

  /// 构建反思消息项
  static Widget buildReflectionMessageItem(ChatMessageModel chatMessage) {
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
  }

  /// 构建推理消息项
  static Widget buildReasoningMessageItem(ChatMessageModel chatMessage) {
    if (chatMessage.message.trim().isEmpty) {
      return Container();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("思考过程", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
        const SizedBox(height: 10),
        Text(chatMessage.message.trimmed(), style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
      ],
    );
  }
}
