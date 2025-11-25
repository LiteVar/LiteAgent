import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/document.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';
import 'package:lite_agent_client/utils/extension/int_extension.dart';

import '../../utils/web_util.dart';
import '../../widgets/pagination/pagination_widget.dart';
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
        PaginationWidget(controller: logic.paginationController)
      ]),
    );
  }

  Widget buildListItem(DocumentDto document) {
    return SizedBox(
      height: 40,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
              child: Center(
                  child: Text(document.name,
                      overflow: TextOverflow.ellipsis, maxLines: 1, style: const TextStyle(color: Color(0xff333333), fontSize: 14)))),
          Expanded(
              child:
                  Center(child: Text((document.wordCount).toShortForm(), style: const TextStyle(color: Color(0xff333333), fontSize: 14)))),
          Expanded(
              child: Center(
            child: document.enableFlag
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
                if (document.fileId.isNotEmpty) ...[
                  InkWell(
                      onTap: () => logic.download(document),
                      child: const Text("下载", style: TextStyle(color: Color(0xff2a82e4), fontSize: 14))),
                  const SizedBox(width: 20),
                ],
                InkWell(
                    onTap: () {
                      WebUtil.openLibraryDocumentUrl(document.datasetId ?? "");
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
}
