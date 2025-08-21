import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../utils/web_util.dart';

class MarkDownTextDialog extends StatelessWidget {
  final String titleText;
  final String contentText;

  const MarkDownTextDialog({super.key, required this.titleText, required this.contentText});

  @override
  Widget build(BuildContext context) {
    return Center(
        child: Container(
      width: 650,
      height: 600,
      decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(16))),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            child: Row(children: [
              Text(titleText, style: const TextStyle(fontSize: 18, color: Color(0xff333333))),
              const Spacer(),
              InkWell(child: const Icon(Icons.close, size: 20, color: Colors.black), onTap: () => Get.back())
            ]),
          ),
          horizontalLine(),
          Expanded(
              child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  child: Container(
                    margin: const EdgeInsets.all(16),
                    child: MarkdownBody(
                        data: contentText,
                        onTapLink: (text, url, title) async {
                          if (url != null) {
                            WebUtil.openUrl(url);
                          }
                        }),
                  )))
        ],
      ),
    ));
  }
}
