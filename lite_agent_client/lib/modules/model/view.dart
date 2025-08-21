import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../models/local_data_model.dart';
import 'logic.dart';

class ModelPage extends StatelessWidget {
  ModelPage({Key? key}) : super(key: key);

  final logic = Get.put(ModelLogic());

  final dialogTitleColor = const Color(0xFFf5f5f5);
  final buttonColor = const Color(0xFF2a82f5);
  final itemBorderColor = const Color(0xFFd9d9d9);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
        color: Colors.white,
        child: Column(
          children: [
            Row(children: [
              const Text('模型管理', style: TextStyle(fontSize: 18, color: Colors.black)),
              const Spacer(),
              _buildNewModelButton(),
            ]),
            const SizedBox(height: 10),
            Expanded(child: Obx(() {
              if (logic.modelList.isNotEmpty) {
                return GridView.count(
                    crossAxisCount: 4,
                    childAspectRatio: 3 / 2.2,
                    children: List.generate(
                      logic.modelList.length,
                      (index) => InkWell(
                        onTap: () => logic.showDetailDialog(logic.modelList[index]),
                        child: _buildModelItem(logic.modelList[index]),
                      ),
                    ));
              } else {
                return Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    SizedBox(height: 330, width: 400, child: Image.asset('assets/images/icon_list_empty.png', fit: BoxFit.contain)),
                    const Text("暂无模型，请创建", style: TextStyle(fontSize: 14, color: Colors.grey)),
                    const SizedBox(height: 40)
                  ],
                );
              }
            }))
          ],
        ),
      ),
    );
  }

  Widget _buildNewModelButton() {
    return TextButton(
        style: ButtonStyle(
            padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(35, 18, 35, 18)),
            backgroundColor: WidgetStateProperty.all(buttonColor),
            shape: WidgetStateProperty.all<RoundedRectangleBorder>(
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            )),
        onPressed: () => logic.showCreateModelDialog(),
        child: const Text('新建模型', style: TextStyle(color: Colors.white, fontSize: 14)));
  }

  Widget _buildModelItem(ModelBean model) {
    return Container(
      margin: const EdgeInsets.all(10),
      padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 10),
      decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(8.0)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                  width: 39, height: 39, decoration: BoxDecoration(color: const Color(0xffcccccc), borderRadius: BorderRadius.circular(6))),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Flexible(
                      child: Text(model.nickName ?? "模型${model.id.lastSixChars}",
                          maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
                    ),
                    Flexible(
                      child: Text(model.name,
                          maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: Color(0xffa6a6a6))),
                    ),
                  ],
                ),
              )
            ],
          ),
          const SizedBox(height: 10),
          Text("${model.type ?? "LLM"}模型",
              maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xffc2c2c2))),
          Flexible(
            child: Text(model.key,
                maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xffc2c2c2))),
          ),
          const Spacer(),
          buildBottomButton(model)
        ],
      ),
    );
  }

  Container buildBottomButton(ModelBean model) {
    return Container(
        margin: const EdgeInsets.symmetric(horizontal: 10),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            InkWell(
              onTap: () {
                logic.showEditModelDialog(model);
              },
              child: Row(children: [
                buildAssetImage("icon_file_text.png", 16, Colors.blue),
                const SizedBox(width: 4),
                const Text('编辑', style: TextStyle(color: Colors.blue, fontSize: 14))
              ]),
            ),
            DropdownButtonHideUnderline(
                child: DropdownButton2(
                    customButton: Row(
                      children: [
                        buildAssetImage("icon_menu.png", 16, Colors.blue),
                        const SizedBox(width: 4),
                        const Text('更多', style: TextStyle(color: Colors.blue, fontSize: 14)),
                      ],
                    ),
                    dropdownStyleData: const DropdownStyleData(
                        width: 80,
                        offset: Offset(0, -10),
                        padding: EdgeInsets.symmetric(vertical: 0),
                        decoration: BoxDecoration(color: Colors.white)),
                    menuItemStyleData: const MenuItemStyleData(
                      height: 40,
                    ),
                    items: const [
                      DropdownMenuItem<String>(
                        value: "delete",
                        child: Center(child: Text("删除", style: TextStyle(fontSize: 14))),
                      )
                    ],
                    onChanged: (value) {
                      if (value == "delete") {
                        logic.removeModel(model.id);
                      }
                    })),
          ],
        ));
  }
}
