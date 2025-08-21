import 'dart:math';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/segment.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';

import '../../widgets/common_widget.dart';
import 'logic.dart';

class SegmentPage extends StatelessWidget {
  SegmentPage({Key? key}) : super(key: key);

  final SegmentLogic logic = Get.put(SegmentLogic());

  @override
  Widget build(BuildContext context) {
    return Stack(children: [
      Container(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              buildTitleSearchRow(),
              const SizedBox(height: 10),
              Expanded(child: Obx(() {
                if (logic.segmentList.isNotEmpty) {
                  return ListView.builder(
                      itemCount: logic.segmentList.length, itemBuilder: (context, index) => buildListItem(logic.segmentList[index]));
                } else {
                  return buildEmptyTipsColumn();
                }
              })),
              Obx(() => Offstage(offstage: logic.totalPage.value == 0, child: buildBottomPageContainer()))
            ],
          )),
      Positioned(
          right: 0, top: 0, bottom: 0, child: Obx(() => Offstage(offstage: !logic.isShowDrawer.value, child: buildSegmentDetailDrawer()))),
    ]);
  }

  Container buildSegmentDetailDrawer() {
    var segment = logic.selectedSegment.value;
    var index = logic.segmentList.indexOf(segment) + 1;
    int number = index + (logic.currentPage.value - 1) * LibraryRepository.SEGMENT_PAGE_SIZE;
    String indexString = number >= 10 ? number.toString() : "0$number";
    if (segment == null) {
      return Container();
    }
    return Container(
      color: Colors.white,
      width: 400,
      child: Stack(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                  margin: const EdgeInsets.only(left: 20, top: 20),
                  child: Text("片段_$indexString", style: const TextStyle(fontSize: 18, color: Color(0xff333333)))),
              Container(
                  margin: const EdgeInsets.only(left: 20, top: 10, bottom: 20),
                  child: Text("字数：${segment.wordCount}", style: const TextStyle(fontSize: 14, color: Color(0xff999999)))),
              Container(height: 0.5, color: Colors.grey),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                    Padding(
                        padding: const EdgeInsets.all(20),
                        child: Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(8)),
                            child: Text(segment.content, style: const TextStyle(fontSize: 14, color: Color(0xff333333)))))
                  ]),
                ),
              )
            ],
          ),
          Container(width: 0.5, color: Colors.grey),
          Positioned(
            right: 20,
            top: 36,
            child: InkWell(
              onTap: () => logic.closeSegmentDrawer(),
              child: Padding(padding: const EdgeInsets.all(5), child: buildAssetImage("icon_close.png", 16, const Color(0xff333333))),
            ),
          )
        ],
      ),
    );
  }

  Widget buildListItem(SegmentDto segment) {
    var index = logic.segmentList.indexOf(segment) + 1;
    int number = index + (logic.currentPage.value - 1) * LibraryRepository.SEGMENT_PAGE_SIZE;
    String indexString = number >= 10 ? number.toString() : "0$number";
    String content = segment.content.replaceAll(RegExp(r'\s+'), ' ').trim();
    return InkWell(
      onTap: () => logic.showSegmentDetailDrawer(segment),
      child: Container(
        margin: const EdgeInsets.only(top: 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text("片段_$indexString  字数：${segment.wordCount}  状态：", style: const TextStyle(fontSize: 12, color: Color(0xff666666))),
                (segment.enableFlag)
                    ? const Text("已激活", style: TextStyle(color: Color(0xff0bb34e), fontSize: 14))
                    : const Text("已冻结", style: TextStyle(color: Color(0xff999999), fontSize: 14))
              ],
            ),
            Container(
              margin: const EdgeInsets.symmetric(vertical: 10),
              child: Text(content,
                  maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
            ),
            Container(color: Colors.grey, height: 0.5)
          ],
        ),
      ),
    );
  }

  Row buildTitleSearchRow() {
    return Row(
      children: [
        InkWell(
          onTap: () => logic.onGoBackButtonClick(),
          child: Container(margin: const EdgeInsets.only(right: 15), child: buildAssetImage("icon_back.png", 20, Colors.black)),
        ),
        Obx(() => Expanded(
            child: Text(logic.documentName.value,
                maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Color(0xff333333), fontSize: 24)))),
        const SizedBox(width: 10),
        Obx(() => Text("片段(共${logic.totalCount}个片段)", style: const TextStyle(color: Color(0xff666666), fontSize: 16))),
        Container(
            width: 320,
            height: 40,
            margin: const EdgeInsets.fromLTRB(10, 0, 10, 0),
            decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(4)),
            child: Center(
              child: TextField(
                  onSubmitted: (string) => logic.onInputSubmit(string),
                  cursorColor: const Color(0xff2A82E4),
                  decoration: const InputDecoration(
                      hintText: '这里是输入的搜索内容',
                      border: InputBorder.none,
                      isDense: true,
                      contentPadding: EdgeInsets.all(16),
                      hintStyle: TextStyle(fontSize: 14, color: Color(0xff999999))),
                  style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
            ))
      ],
    );
  }

  Container buildBottomPageContainer() {
    if (logic.totalPage.value < 1) return Container();
    return Container(
      margin: const EdgeInsets.fromLTRB(20, 20, 60, 0),
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
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(height: 330, width: 400, child: Image.asset('assets/images/icon_list_empty.png', fit: BoxFit.contain)),
        const Text("暂无数据", style: TextStyle(fontSize: 14, color: Colors.grey), textAlign: TextAlign.center),
        const SizedBox(height: 40)
      ],
    );
  }
}
