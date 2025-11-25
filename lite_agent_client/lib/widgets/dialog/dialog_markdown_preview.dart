import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:get/get.dart';

import '../../utils/alarm_util.dart';
import '../../utils/web_util.dart';
import '../common_widget.dart';

class MarkdownPreviewDialog extends StatelessWidget {
  final String titleText;
  final String contentText;

  const MarkdownPreviewDialog({super.key, required this.titleText, required this.contentText});

  @override
  Widget build(BuildContext context) {
    return Center(
        child: Container(
      width: 800,
      height: 620,
      decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(16))),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(),
          _buildMarkdown(),
          Container(margin: const EdgeInsets.symmetric(horizontal: 20), child: horizontalLine()),
          _buildFooter(),
        ],
      ),
    ));
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        color: Color(0xfff5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(16), topRight: Radius.circular(16)),
      ),
      child: Row(children: [
        Expanded(child: Text(titleText, style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 16, color: Color(0xff333333)))),
        InkWell(child: const Icon(Icons.close, size: 20, color: Colors.black), onTap: () => Get.back())
      ]),
    );
  }

  Widget _buildMarkdown() {
    return Expanded(
        child: Scrollbar(
            thumbVisibility: true,
            child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                child: Container(
                  margin: const EdgeInsets.all(20),
                  child: MarkdownBody(
                      data: contentText,
                      onTapLink: (text, url, title) async {
                        if (url != null) {
                          WebUtil.openUrl(url);
                        }
                      }),
                ))));
  }

  Widget _buildFooter() {
    return Container(
      padding: const EdgeInsets.all(16),
      margin: const EdgeInsets.symmetric(horizontal: 4),
      child: Row(children: [
        InkWell(
          onTap: () async {
            await Clipboard.setData(ClipboardData(text: contentText));
            AlarmUtil.showAlertToast("复制成功");
          },
          child: const Text("复制内容", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
        ),
        const Spacer(),
        OutlinedButton(
          onPressed: () => Get.back(),
          style: OutlinedButton.styleFrom(
            foregroundColor: const Color(0xff666666),
            side: const BorderSide(color: Color(0xff666666)),
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
          ),
          child: const Text("关闭", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
        )
      ]),
    );
  }
}
