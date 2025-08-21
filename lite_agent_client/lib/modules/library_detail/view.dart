import 'dart:io';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/modules/document/view.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';

import '../../utils/web_util.dart';
import '../../widgets/common_widget.dart';
import '../retrieval/view.dart';
import '../segment/view.dart';
import 'logic.dart';

class LibraryDetailPage extends StatelessWidget {
  LibraryDetailPage({Key? key}) : super(key: key);

  final LibraryDetailLogic logic = Get.put(LibraryDetailLogic());

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        buildTitleContainer(),
        Container(color: Colors.grey, height: 0.5),
        Expanded(
            child: Container(
                color: Colors.white,
                child: Row(
                  children: [
                    Obx(() => Column(
                          children: [
                            const SizedBox(height: 20),
                            createPageItem(
                                "icon_library_segment.png",
                                logic.currentPage.value == LibraryDetailLogic.PAGE_DOCUMENT ||
                                    logic.currentPage.value == LibraryDetailLogic.PAGE_SEGMENT,
                                () => {logic.switchPage(LibraryDetailLogic.PAGE_DOCUMENT)}),
                            createPageItem("icon_library_retrieval.png", logic.currentPage.value == LibraryDetailLogic.PAGE_RETRIEVAL,
                                () => {logic.switchPage(LibraryDetailLogic.PAGE_RETRIEVAL)}),
                            createPageItem(
                              "icon_library_api.png",
                              false,
                              () {
                                WebUtil.openLibraryDocumentApiUrl(logic.libraryId);
                              }.throttle(),
                            ),
                            createPageItem(
                              "icon_library_setting.png",
                              false,
                              () {
                                WebUtil.openLibrarySettingUrl(logic.libraryId);
                              }.throttle(),
                            ),
                          ],
                        )),
                    Container(color: Colors.grey, width: 0.5),
                    Expanded(child: Obx(() {
                      if (logic.currentPage.value == LibraryDetailLogic.PAGE_DOCUMENT) {
                        return DocumentPage();
                      } else if (logic.currentPage.value == LibraryDetailLogic.PAGE_SEGMENT) {
                        return SegmentPage();
                      } else if (logic.currentPage.value == LibraryDetailLogic.PAGE_RETRIEVAL) {
                        return RetrievalPage();
                      } else {
                        return Container();
                      }
                    })
                        //Expanded(child: Obx(() => logic.currentPage.value == LibraryDetailLogic.PAGE_SEGMENT ? SegmentPage() :
                        // RetrievalPage()))
                        )
                  ],
                )))
      ],
    );
  }

  Widget buildTitleContainer() {
    return Container(
      color: Colors.white,
      height: 60,
      child: Row(
        children: [
          Obx(() => Offstage(offstage: logic.isFullScreen.value || Platform.isWindows, child: const SizedBox(width: 50))),
          Container(
            margin: const EdgeInsets.only(left: 10, right: 5),
            child: InkWell(
                onTap: () => logic.goBack(),
                child: Container(padding: const EdgeInsets.all(13), child: buildAssetImage("icon_back.png", 16, Colors.black))),
          ),
          Container(
            width: 32,
            height: 32,
            margin: const EdgeInsets.only(right: 10),
            decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(6))),
            child: Center(child: buildAssetImage("icon_document.png", 15, Colors.black)),
          ),
          Obx(() => Text(logic.libraryName.value, style: const TextStyle(color: Color(0xff333333), fontSize: 14)))
        ],
      ),
    );
  }

  Widget createPageItem(String iconFileName, bool isSelect, Function()? onTap) {
    var itemColor = isSelect ? Colors.blue : Colors.grey;
    return Container(
        width: 60,
        height: 60,
        child: InkWell(
          onTap: onTap,
          child: Center(
              child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [buildAssetImage(iconFileName, 20, itemColor)],
          )),
        ));
  }
}
