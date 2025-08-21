import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

class ModelDetailDialog extends StatelessWidget {
  final ModelBean model;

  ModelDetailDialog({required this.model});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 538,
        constraints: BoxConstraints(minHeight: 238, maxHeight: MediaQuery.of(context).size.height * 0.8),
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            buildTitleContainer(),
            Flexible(
              child: SingleChildScrollView(
                child: Container(
                    margin: const EdgeInsets.all(16),
                    child: Column(
                      mainAxisSize: MainAxisSize.min, // 防止内容区域过度拉伸
                      crossAxisAlignment: CrossAxisAlignment.center, // 水平居中
                      children: [
                        buildInfoRow("模型名称:", model.name),
                        buildInfoRow("连接别名:", model.nickName ?? "模型${model.id.lastSixChars}"),
                        buildInfoRow("BaseURL:", model.url),
                        buildInfoRow("API Key:", model.key),
                      ],
                    )),
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget buildInfoRow(String title, String content) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(title, textAlign: TextAlign.right, style: const TextStyle(fontSize: 14, color: Color(0xff8c8c8c))),
          ),
          const SizedBox(width: 10),
          Expanded(child: Text(content, style: const TextStyle(fontSize: 14, color: Color(0xff383838))))
        ],
      ),
    );
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        const Text("模型详情"),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () => Get.back(),
        )
      ]),
    );
  }
}
