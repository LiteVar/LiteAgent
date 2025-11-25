import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/segment.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';

import '../../widgets/common_widget.dart';
import '../../widgets/pagination/pagination_widget.dart';
import 'logic.dart';

class SegmentPage extends StatelessWidget {
  SegmentPage({Key? key}) : super(key: key);

  final SegmentLogic logic = Get.put(SegmentLogic());

  /// 创建高亮文本
  Widget buildHighlightText(String text, String keyword, TextStyle baseStyle, {int? maxLines, TextOverflow? overflow}) {
    if (keyword.isEmpty) {
      return Text(text, style: baseStyle, maxLines: maxLines, overflow: overflow);
    }

    List<TextSpan> spans = [];
    String lowerText = text.toLowerCase();
    String lowerKeyword = keyword.toLowerCase();
    int start = 0;

    while (start < text.length) {
      int index = lowerText.indexOf(lowerKeyword, start);
      if (index == -1) {
        spans.add(TextSpan(text: text.substring(start), style: baseStyle));
        break;
      }

      if (index > start) {
        spans.add(TextSpan(text: text.substring(start, index), style: baseStyle));
      }

      spans.add(TextSpan(
        text: text.substring(index, index + keyword.length),
        style: baseStyle.copyWith(backgroundColor: const Color(0xffFFEB3B), fontWeight: FontWeight.bold),
      ));

      start = index + keyword.length;
    }

    return RichText(
      text: TextSpan(children: spans),
      maxLines: maxLines,
      overflow: overflow ?? TextOverflow.clip,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Stack(children: [
      Container(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              buildTitleRow(),
              Container(margin: const EdgeInsets.symmetric(vertical: 16), child: horizontalLine()),
              buildSearchRow(),
              const SizedBox(height: 10),
              Expanded(child: Obx(() {
                if (logic.segmentList.isNotEmpty) {
                  return ListView.builder(
                      itemCount: logic.segmentList.length, itemBuilder: (context, index) => buildListItem(logic.segmentList[index]));
                } else {
                  return buildEmptyTipsColumn();
                }
              })),
              PaginationWidget(margin: const EdgeInsets.all(20), controller: logic.paginationController)
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
                            child: Obx(() => buildHighlightText(
                              segment.content,
                              logic.keyWord.value,
                              const TextStyle(fontSize: 14, color: Color(0xff333333)),
                            ))))
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
              child: Obx(() => buildHighlightText(
                content,
                logic.keyWord.value,
                const TextStyle(fontSize: 14, color: Color(0xff333333)),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              )),
            ),
            Container(color: Colors.grey, height: 0.5)
          ],
        ),
      ),
    );
  }

  Row buildTitleRow() {
    return Row(
      children: [
        InkWell(
          onTap: () => logic.onGoBackButtonClick(),
          child: Container(margin: const EdgeInsets.only(right: 10), child: buildAssetImage("icon_back.png", 20, Colors.black)),
        ),
        Obx(() => Expanded(
            child: Text(logic.documentName.value,
                maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Color(0xff333333), fontSize: 18)))),
        if (logic.fileId.isNotEmpty) ...[
          const SizedBox(width: 10),
          InkWell(
            onTap: () => logic.downloadDocumentFile(),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(border: Border.all(color: const Color(0xffc7c7c7)), borderRadius: BorderRadius.circular(11.0)),
              child: const Text("下载Markdown文档", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
            ),
          )
        ]
      ],
    );
  }

  Row buildSearchRow() {
    return Row(
      children: [
        Obx(() => Text("片段（共 ${logic.totalCount} 个片段）", style: const TextStyle(color: Color(0xff333333), fontSize: 16))),
        const Spacer(),
        Container(
            width: 240,
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
