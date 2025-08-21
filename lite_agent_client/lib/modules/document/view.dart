import 'dart:math';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/document.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';
import 'package:lite_agent_client/utils/extension/int_extension.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../utils/web_util.dart';
import '../library_detail/logic.dart';
import 'logic.dart';

class DocumentPage extends StatelessWidget {
  DocumentPage({Key? key}) : super(key: key);

  final DocumentLogic logic = Get.put(DocumentLogic());

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Obx(() => Text("文档列表(共${logic.totalCount}个)", style: const TextStyle(color: Color(0xff333333), fontSize: 24))),
        buildListTitleContainer(),
        Container(height: 0.5, color: Colors.grey, margin: const EdgeInsets.symmetric(horizontal: 5)),
        Obx(() => Expanded(
            child: ListView.builder(
                itemCount: logic.documentList.length, itemBuilder: (context, index) => buildListItem(logic.documentList[index])))),
        Obx(() => buildBottomPageContainer())
      ]),
    );
  }

  Widget buildListItem(DocumentDto document) {
    return SizedBox(
      height: 40,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(child: Center(child: Text(document.name, style: const TextStyle(color: Color(0xff333333), fontSize: 14)))),
          Expanded(
              child: Center(
                  child: Text((document.wordCount ?? 0).toShortForm(), style: const TextStyle(color: Color(0xff333333), fontSize: 14)))),
          Expanded(
              child: Center(
            child: document.enableFlag ?? false
                ? const Text("已激活", style: TextStyle(color: Color(0xff0bb34e), fontSize: 14))
                : const Text("已冻结", style: TextStyle(color: Color(0xff999999), fontSize: 14)),
          )),
          Expanded(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                InkWell(
                    onTap: () => logic.switchToSegment(document),
                    child: const Text("详情", style: TextStyle(color: Color(0xff2a82e4), fontSize: 14))),
                const SizedBox(width: 20),
                InkWell(
                    onTap: () {
                      WebUtil.openLibraryDocumentUrl(document.datasetId??"");
                    }.throttle(),
                    child: const Text("编辑", style: TextStyle(color: Color(0xff2a82e4), fontSize: 14))),
              ],
            ),
          )
        ],
      ),
    );
  }

  Container buildListTitleContainer() {
    return Container(
      margin: const EdgeInsets.fromLTRB(5, 15, 5, 0),
      color: const Color(0xfffafafa),
      height: 40,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          const Text("名称", style: TextStyle(color: Colors.black, fontSize: 14)),
          Container(width: 0.5, height: 20, color: Colors.grey),
          const Text("字数", style: TextStyle(color: Colors.black, fontSize: 14)),
          Container(width: 0.5, height: 20, color: Colors.grey),
          const Text("状态", style: TextStyle(color: Colors.black, fontSize: 14)),
          Container(width: 0.5, height: 20, color: Colors.grey),
          const Text("操作", style: TextStyle(color: Colors.black, fontSize: 14)),
        ],
      ),
    );
  }

  Container buildBottomPageContainer() {
    if (logic.totalPage.value < 1) return Container();
    return Container(
      margin: const EdgeInsets.fromLTRB(20, 20, 60, 20),
      child: Row(mainAxisAlignment: MainAxisAlignment.end, children: [
        Row(
          children: [
            const SizedBox(width: 10),
            InkWell(
              onTap: () => logic.loadData(logic.currentPage.value - 1),
              child: buildAssetImage("icon_button_left.png", 30, const Color(0xff666666)),
            )
          ],
        ),
        ...List.generate(
          min(logic.pageButtonCount, logic.totalPage.value), // 动态生成的小组件数量
              (index) {
            var page = logic.pageButtonNumberStart + index;
            return Row(
              children: [
                const SizedBox(width: 10),
                InkWell(
                  onTap: () => logic.loadData(page),
                  child: Container(
                    decoration: logic.currentPage.value == (page)
                        ? BoxDecoration(color: const Color(0xff337fe3), borderRadius: BorderRadius.circular(4))
                        : BoxDecoration(border: Border.all(color: const Color(0xfff5f5f5)), borderRadius: BorderRadius.circular(4)),
                    width: 30,
                    height: 30,
                    child: Center(
                        child: Text("$page",
                            style:
                            TextStyle(fontSize: 16, color: logic.currentPage.value == page ? Colors.white : const Color(0xff666666)))),
                  ),
                )
              ],
            );
          },
        ),
        Row(
          children: [
            const SizedBox(width: 10),
            InkWell(
              onTap: () => logic.loadData(logic.currentPage.value + 1),
              child: buildAssetImage("icon_button_right.png", 30, const Color(0xff666666)),
            )
          ],
        ),
      ]),
    );
  }
}
