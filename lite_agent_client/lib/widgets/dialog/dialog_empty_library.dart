import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../utils/web_util.dart';

class EmptyLibraryDialog extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
        child: Container(
      width: 538,
      height: 200,
      decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
            decoration: const BoxDecoration(
              color: Color(0xFFf5f5f5),
              borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
            ),
            child: Row(children: [
              const Text("添加知识库"),
              const Spacer(),
              InkWell(child: const Icon(Icons.close, size: 16, color: Colors.black), onTap: () => Get.back())
            ]),
          ),
          const SizedBox(height: 16),
          Container(
              margin: const EdgeInsets.only(left: 16),
              child: const Text('还没有添加过知识库，如需添加知识库，请前往网页版知识库管理进行添加。', style: TextStyle(fontSize: 14.0, color: Color(0xff666666)))),
          const SizedBox(height: 4.0),
          InkWell(
            onTap: () {
              WebUtil.openLibraryTabUrl();
            },
            child: Container(
                margin: const EdgeInsets.only(left: 16),
                child: const Text('点击进入网页版知识库管理', style: TextStyle(fontSize: 14.0, color: Colors.blue))),
          )
        ],
      ),
    ));
  }
}
