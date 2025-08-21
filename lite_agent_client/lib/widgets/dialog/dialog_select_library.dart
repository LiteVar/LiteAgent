import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/uitl/snowflake_uitl.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';

import '../../models/local_data_model.dart';
import '../common_widget.dart';
import 'dialog_tool_edit.dart';

class SelectLibraryDialog extends StatelessWidget {
  final void Function(LibraryDto library) onConfirm;

  SelectLibraryDialog({super.key, required this.selectLibraryId, required this.onConfirm});

  //ui
  var borderRadius = 16.0;

  //data
  var selectLibraryId = <String>[];
  var libraryList = <LibraryDto>[].obs;

  Future<void> initData() async {
    var list = await libraryRepository.getLibraryListForSelectDialog();
    if (list != null) {
      libraryList.assignAll(list);
    }
  }

  void toggleSelectStatus(LibraryDto library) {
    if (selectLibraryId.contains(library.id)) {
      selectLibraryId.remove(library.id);
    } else {
      selectLibraryId.add(library.id);
    }
    libraryList.refresh();
    onConfirm(library);
  }

  @override
  Widget build(BuildContext context) {
    initData();
    return Center(
        child: Container(
            width: 586,
            height: 560,
            padding: const EdgeInsets.only(bottom: 10),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(borderRadius))),
            child: Column(children: [
              buildTitleContainer(),
              Expanded(
                  child: Obx(() => ListView.builder(
                        itemCount: libraryList.length,
                        itemBuilder: (context, index) => buildListItem(libraryList[index]),
                      ))),
            ])));
  }

  Container buildTitleContainer() {
    return Container(
      margin: const EdgeInsets.only(bottom: 15),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(borderRadius), topRight: Radius.circular(borderRadius)),
      ),
      child: Row(children: [
        const Text("添加知识库"),
        const Spacer(),
        InkWell(
            onTap: () => Get.back(),
            child: Container(margin: const EdgeInsets.only(right: 2), child: buildAssetImage("icon_close.png", 16, Colors.black)))
      ]),
    );
  }

  Container buildListItem(LibraryDto library) {
    var isSelected = selectLibraryId.contains(library.id);
    var buttonText = isSelected ? "移除" : "添加";
    var buttonTextColor = isSelected ? Color(0xffF24E4E) : Color(0xff2A82E4);
    var buttonColor = isSelected ? Color(0xffFFE8E8) : Color(0xffE8F2FF);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                  width: 36,
                  height: 36,
                  padding: const EdgeInsets.all(10),
                  decoration: const BoxDecoration(color: Color(0xffe8e8e8), borderRadius: BorderRadius.all(Radius.circular(4))),
                  child: buildAssetImage("icon_document.png", 0, Colors.black)),
              Expanded(
                  child: Container(
                      margin: const EdgeInsets.symmetric(horizontal: 15),
                      child: Text(library.name, style: const TextStyle(fontSize: 16, color: Colors.black)))),
              TextButton(
                  style: ButtonStyle(
                      padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 16)),
                      backgroundColor: WidgetStateProperty.all(buttonColor),
                      shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                        RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                      )),
                  onPressed: () => toggleSelectStatus(library),
                  child: Text(buttonText, style: TextStyle(color: buttonTextColor, fontSize: 14)))

              //buildCommonTextBlueButton(buttonText, () => toggleSelectStatus(tool.id, isSelected))
            ],
          ),
          Container(margin: const EdgeInsets.symmetric(vertical: 15), child: horizontalLine()),
        ],
      ),
    );
  }
}
