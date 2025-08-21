import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';

class ToolDetailDialog extends StatelessWidget {
  final ToolBean tool;

  ToolDetailDialog({required this.tool});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 638,
        height: 538,
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
        child: Column(
          children: [
            buildTitleContainer(),
            Expanded(
              child: SingleChildScrollView(
                  child: Container(
                margin: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 20),
                    Center(
                      child: Column(
                        children: [
                          Container(
                              margin: const EdgeInsets.all(20),
                              child: Text(tool.name, style: const TextStyle(fontSize: 14, color: Color(0xFF333333)))),
                          Container(
                              margin: const EdgeInsets.symmetric(horizontal: 40),
                              child: Text(tool.description, style: const TextStyle(fontSize: 14, color: Color(0xFF333333)))),
                          Container(margin: const EdgeInsets.symmetric(vertical: 10, horizontal: 10), child: const Divider(height: 0.1))
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        buildInfoRow(title: "Schema类型:", content: tool.schemaType),
                        buildInfoRow(title: "Schema文稿:", content: tool.schemaText),
                        buildInfoRow(title: "API Key类型:", content: tool.apiType),
                        buildInfoRow(title: "Api Key值:", content: tool.apiText),
                        buildInfoRow(title: "Auto MultiAgents:", content: tool.supportMultiAgent ?? false ? "支持" : "不支持"),
                      ],
                    )
                  ],
                ),
              )),
            )
          ],
        ),
      ),
    );
  }

  Widget buildInfoRow({required String title, required String content}) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
              width: 125, child: Text(title, textAlign: TextAlign.right, style: const TextStyle(fontSize: 14, color: Color(0xFF333333)))),
          Expanded(
            child: Container(
              margin: const EdgeInsets.only(left: 10),
              child: Text(content, style: const TextStyle(fontSize: 14, color: Color(0xFF999999))),
            ),
          )
        ],
      ),
    );
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        const Text("工具详情"),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () => Get.back(),
        )
      ]),
    );
  }
}
