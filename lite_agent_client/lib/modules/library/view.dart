import 'dart:math';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/utils/extension/int_extension.dart';

import '../../widgets/common_widget.dart';
import 'logic.dart';

class LibraryPage extends StatelessWidget {
  LibraryPage({super.key});

  final LibraryLogic logic = Get.put(LibraryLogic());

  var itemSpacingWidth = 10.0;
  var pageButtonNumberStart = 1;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          buildTitleContainer(),
          Container(color: Colors.grey, height: 0.5),
          Expanded(child: Obx(() {
            if (logic.libraryList.isNotEmpty) {
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(
                    child: Container(
                      margin: const EdgeInsets.all(20),
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          return SingleChildScrollView(
                              physics: const AlwaysScrollableScrollPhysics(),
                              child: Wrap(
                                  spacing: itemSpacingWidth,
                                  runSpacing: itemSpacingWidth,
                                  children: List.generate(logic.libraryList.length,
                                      (index) => _buildLibraryItem(constraints.maxWidth, logic.libraryList[index]))));
                        },
                      ),
                    ),
                  ),
                  buildBottomPageContainer()
                ],
              );
            } else {
              return buildEmptyTipsColumn();
            }
          })),
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

  Column buildEmptyTipsColumn() {
    String tips = logic.keyWord.isEmpty ? "暂无添加知识库" : "没有找到相关知识库";
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(height: 330, width: 400, child: Image.asset('assets/images/icon_list_empty.png', fit: BoxFit.contain)),
        Text(tips, style: const TextStyle(fontSize: 14, color: Colors.grey), textAlign: TextAlign.center),
        const SizedBox(height: 40)
      ],
    );
  }

  Container buildTitleContainer() {
    return Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
        child: Row(children: [
          const Text('知识库管理', style: TextStyle(fontSize: 18, color: Colors.black)),
          Expanded(
              child: Container(
                  height: 40,
                  margin: const EdgeInsets.fromLTRB(20, 0, 10, 0),
                  decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(8)),
                  child: Center(
                    child: TextField(
                        onSubmitted: (string) => logic.searchKeyWord(string),
                        controller: null,
                        decoration: const InputDecoration(
                            hintText: '搜索你的知识库', border: InputBorder.none, isDense: true, contentPadding: EdgeInsets.all(16)),
                        style: const TextStyle(fontSize: 14, color: Color(0xff999999))),
                  ))),
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(24, 18, 24, 18)),
                  backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)))),
              onPressed: () => logic.refreshList(),
              child: const Text('同步', style: TextStyle(color: Colors.white, fontSize: 14))),
        ]));
  }

  Widget _buildLibraryItem(double maxWidth, LibraryDto library) {
    // 接收最大宽度参数
    final itemWidth = (maxWidth - itemSpacingWidth * 3) / 4; // 计算子项宽度（减去间距）
    String description = (library.description ?? "").replaceAll(RegExp(r'\s+'), ' ').trim();
    return InkWell(
      child: Container(
        width: itemWidth, // 设置容器的宽度
        padding: const EdgeInsets.fromLTRB(15, 10, 15, 10),
        decoration: BoxDecoration(border: Border.all(color: const Color(0xFFd9d9d9)), borderRadius: BorderRadius.circular(8.0)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(6))),
                  child: Center(child: buildAssetImage("icon_document.png", 15, Colors.black)),
                ),
                const SizedBox(width: 12),
                Expanded(
                    child: Text(library.name,
                        maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16, color: Colors.black))),
                Offstage(
                    offstage: !(library.shareFlag ?? false),
                    child: Container(
                        margin: const EdgeInsets.only(left: 4),
                        padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                        decoration: const BoxDecoration(color: Color(0xffe7f2fe), borderRadius: BorderRadius.all(Radius.circular(8))),
                        child: const Text("已分享", style: TextStyle(fontSize: 12, color: Color(0xff2A82E4)))))
              ],
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 5.0,
              runSpacing: 5.0,
              children: [
                buildLabelContainer("文件:${library.docCount}"),
                buildLabelContainer("Agent:${library.agentCount}"),
                buildLabelContainer("字数:${library.wordCount?.toShortForm()}")
              ],
            ),
            const SizedBox(height: 8),
            SizedBox(
                height: 60,
                child: Text(description,
                    maxLines: 3, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xff666666))))
          ],
        ),
      ),
      onTap: () => logic.jumpToDetail(library),
    );
  }

  Container buildLabelContainer(String label) {
    return Container(
        padding: const EdgeInsets.symmetric(vertical: 2, horizontal: 5),
        //decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(8)),
        decoration:
            BoxDecoration(border: Border.all(color: const Color(0xff333333)), borderRadius: const BorderRadius.all(Radius.circular(4))),
        child: Text(label, maxLines: 1, style: const TextStyle(fontSize: 12, color: Color(0xff666666))));
  }
}
