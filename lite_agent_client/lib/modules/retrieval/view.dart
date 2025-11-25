import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/retrieval_result.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../models/dto/retrieval_record.dart';
import '../../widgets/pagination/pagination_widget.dart';
import 'logic.dart';

class RetrievalPage extends StatelessWidget {
  RetrievalPage({Key? key}) : super(key: key);

  final RetrievalLogic logic = Get.put(RetrievalLogic());

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        Expanded(
            child: Container(
          padding: const EdgeInsets.all(20),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text("检索测试", style: TextStyle(color: Color(0xff333333), fontSize: 24)),
            Container(
                margin: const EdgeInsets.only(top: 15, bottom: 10),
                child: const Text("根据给定的查询文本，测试知识库的命中效果。", style: TextStyle(color: Color(0xff666666), fontSize: 16))),
            buildInputContainer(),
            Container(
                margin: const EdgeInsets.only(top: 20, bottom: 15),
                child: const Text("检索记录", style: TextStyle(color: Color(0xff333333), fontSize: 24))),
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 5),
              color: const Color(0xfffafafa),
              height: 40,
              child: Row(
                children: [
                  const SizedBox(width: 100, child: Center(child: Text("来源", style: TextStyle(color: Colors.black, fontSize: 14)))),
                  Container(width: 0.5, height: 20, color: Colors.grey),
                  const Expanded(child: Center(child: Text("检索文本", style: TextStyle(color: Colors.black, fontSize: 14)))),
                  Container(width: 0.5, height: 20, color: Colors.grey),
                  const Expanded(child: Center(child: Text("检索记录", style: TextStyle(color: Colors.black, fontSize: 14)))),
                ],
              ),
            ),
            Container(margin: const EdgeInsets.symmetric(horizontal: 5), child: horizontalLine()),
            Expanded(
                child: Obx(() => ListView.builder(
                    itemCount: logic.recordList.length,
                    itemBuilder: (context, index) => buildRecordListItem(index, logic.recordList[index])))),
            PaginationWidget(controller: logic.paginationController, margin: const EdgeInsets.all(20))
          ]),
        )),
        verticalLine(),
        Expanded(child: buildRightResultContainer())
      ],
    );
  }

  Widget buildRecordListItem(int index, RetrievalRecordDto record) {
    return Obx(() {
      var backgroundColor = logic.recordHoverItemId.value == index.toString() ? const Color(0xfff5f5f5) : Colors.transparent;
      var retrieveType = record.retrieveType == "TEST" ? "检索测试" : "Agent";
      return MouseRegion(
          onEnter: (event) => logic.recordHoverItemId.value = index.toString(),
          onExit: (event) => logic.recordHoverItemId.value = "",
          child: InkWell(
              onTap: () => logic.getRetrieveHistory(record.id),
              child: Container(
                decoration: BoxDecoration(color: backgroundColor),
                height: 40,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    SizedBox(
                        width: 100,
                        child: Center(child: Text(retrieveType, style: const TextStyle(color: Color(0xff333333), fontSize: 12)))),
                    Expanded(
                      child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                          child: Center(
                            child: Text(
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                record.content,
                                style: const TextStyle(color: Color(0xff333333), fontSize: 12)),
                          )),
                    ),
                    Expanded(
                      child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 10),
                          child: Center(
                            child: Text(
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                record.createTime,
                                style: const TextStyle(color: Color(0xff333333), fontSize: 12)),
                          )),
                    ),
                  ],
                ),
              )));
    });
  }

  Container buildInputContainer() {
    return Container(
      height: 160,
      decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(12)),
      padding: const EdgeInsets.all(12),
      child: Stack(
        children: [
          SizedBox(
            height: 100,
            child: TextField(
                //onSubmitted: (value) => logic.startRetrieval(),
                controller: logic.retrieveController,
                maxLines: null,
                maxLength: 200,
                keyboardType: TextInputType.multiline,
                style: const TextStyle(color: Color(0xff333333), fontSize: 12),
                decoration: const InputDecoration(
                  contentPadding: EdgeInsets.zero,
                  border: InputBorder.none,
                  hintText: '请输入查询文本进行测试',
                  hintStyle: TextStyle(color: Color(0xff999999), fontSize: 12),
                  counterText: "",
                )),
          ),
          Positioned(
              right: 0,
              bottom: 0,
              child: Obx(() => InkWell(
                  onTap: () => logic.startRetrieval(),
                  child: Container(
                    decoration: BoxDecoration(
                        color: logic.enableRetrieval.value ? const Color(0xff0082e0) : const Color(0x4d0082e0),
                        borderRadius: BorderRadius.circular(8)),
                    padding: const EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                    child: const Text("开始测试", style: TextStyle(color: Colors.white, fontSize: 12)),
                  )))),
          Positioned(
              left: 5,
              bottom: 5,
              child: Obx(() => Text("${logic.textCount}/200", style: const TextStyle(color: Color(0xff999999), fontSize: 12))))
        ],
      ),
    );
  }

  Container buildRightResultContainer() {
    return Container(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("测试结果", style: TextStyle(color: Color(0xff333333), fontSize: 24)),
          const SizedBox(height: 15),
          Obx(() => Offstage(
              offstage: !logic.showNoResultTips.value,
              child: const Text("没有找到相关搜索结果", style: TextStyle(color: Color(0xff999999), fontSize: 14)))),
          Expanded(
            child: Obx(() => ListView.separated(
                controller: logic.resultScrollController,
                itemCount: logic.resultList.length,
                itemBuilder: (context, index) => buildResultListItem(index, logic.resultList[index]),
                separatorBuilder: (BuildContext context, int index) => const SizedBox(height: 10))),
          )
        ],
      ),
    );
  }

  Container buildResultListItem(int index, RetrievalResultDto result) {
    String documentName = result.documentName ?? "";
    documentName = documentName.isEmpty ? "链接文档" : documentName;
    return Container(
      decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(12)),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text("#${index + 1} | 关联度 ${result.score.toStringAsFixed(2)}", style: const TextStyle(color: Color(0xff2a82e4), fontSize: 12)),
          Container(
            margin: const EdgeInsets.symmetric(vertical: 5),
            child: Text(result.content ?? "", style: const TextStyle(color: Color(0xff333333), fontSize: 14)),
          ),
          Row(
            children: [
              Text("token：${result.tokenCount}", style: const TextStyle(color: Color(0xff999999), fontSize: 12)),
              const SizedBox(width: 20),
              buildAssetImage("icon_library_segment.png", 14, const Color(0xff999999)),
              Flexible(
                child: Text(
                  documentName,
                  overflow: TextOverflow.ellipsis,
                  maxLines: 1,
                  style: const TextStyle(color: Color(0xff999999), fontSize: 12),
                ),
              ),
              const SizedBox(width: 20),
              if (result.fileId.isNotEmpty) ...[
                InkWell(
                  onTap: () => logic.showDocumentDetail(result.fileId, result.documentName),
                  child: const Text("查看原文", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                ),
                const SizedBox(width: 20),
                InkWell(
                  onTap: () => logic.downloadDocumentFile(result.fileId),
                  child: const Text("下载源文件", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                )
              ],
            ],
          )
        ],
      ),
    );
  }
}
