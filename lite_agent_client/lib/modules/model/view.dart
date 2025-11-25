import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';

import 'package:lite_agent_client/widgets/common_widget.dart';

import 'logic.dart';

class ModelPage extends StatelessWidget {
  ModelPage({Key? key}) : super(key: key);

  final logic = Get.put(ModelLogic());

  final dialogTitleColor = const Color(0xFFf5f5f5);
  final buttonColor = const Color(0xFF2a82f5);
  final itemBorderColor = const Color(0xFFd9d9d9);

  final itemSpacingWidth = 20.0;

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
                return Container(
                  margin: const EdgeInsets.all(15),
                  child: LayoutBuilder(
                    builder: (context, constraints) {
                      return ScrollConfiguration(
                          behavior: ScrollConfiguration.of(context).copyWith(scrollbars: false),
                          child: SingleChildScrollView(
                              physics: const AlwaysScrollableScrollPhysics(),
                              child: Align(
                                alignment: Alignment.centerLeft,
                                child: Wrap(
                                    spacing: itemSpacingWidth,
                                    runSpacing: itemSpacingWidth,
                                    children: List.generate(
                                      logic.modelList.length,
                                      (index) => InkWell(
                                        onTap: () => logic.showDetailDialog(logic.modelList[index]),
                                        child: _buildModelItem(constraints.maxWidth, logic.modelList[index]),
                                      ),
                                    )))));
                    },
                  ),
                );
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
    return DropdownButtonHideUnderline(
      child: DropdownButton2<String>(
        customButton: TextButton(
          style: ButtonStyle(
            padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(24, 18, 16, 18)),
            backgroundColor: WidgetStateProperty.all(buttonColor),
            shape: WidgetStateProperty.all<RoundedRectangleBorder>(
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
          ),
          onPressed: null,
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('新建模型', style: TextStyle(color: Colors.white, fontSize: 14)),
              SizedBox(width: 8),
              Icon(Icons.keyboard_arrow_down, color: Colors.white, size: 16),
            ],
          ),
        ),
        dropdownStyleData: const DropdownStyleData(
          width: null, // 使用null让宽度自动匹配按钮宽度
          offset: Offset(0, -5),
          padding: EdgeInsets.symmetric(vertical: 6),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.all(Radius.circular(8)),
            boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 8, offset: Offset(0, 4))],
          ),
        ),
        menuItemStyleData: const MenuItemStyleData(height: 40, padding: EdgeInsets.zero),
        items: const [
          DropdownMenuItem<String>(
            value: "create",
            child: Center(child: Text("新建模型", style: TextStyle(fontSize: 14, color: Colors.black))),
          ),
          DropdownMenuItem<String>(
            value: "import",
            child: Center(child: Text("导入模型", style: TextStyle(fontSize: 14, color: Colors.black))),
          ),
        ],
        onChanged: (value) {
          if (value == "create") {
            logic.showCreateModelDialog();
          } else if (value == "import") {
            logic.importModel();
          }
        },
      ),
    );
  }

  Widget _buildModelItem(double maxWidth, ModelData model) {
    // 计算子项宽度（减去间距）
    final itemWidth = (maxWidth - itemSpacingWidth * 3) / 4;
    return Container(
      width: itemWidth,
      padding: const EdgeInsets.fromLTRB(15, 10, 15, 10),
      decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(8.0)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
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
                    Text(model.alias ?? "",
                        maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
                    Text(model.name,
                        maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: Color(0xffa6a6a6))),
                  ],
                ),
              )
            ],
          ),
          const SizedBox(height: 10),
          Text("${model.type ?? ModelValidator.LLM}模型",
              maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xffc2c2c2))),
          Text(model.key, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xffc2c2c2))),
          const SizedBox(height: 10),
          buildBottomButton(model)
        ],
      ),
    );
  }

  Container buildBottomButton(ModelData model) {
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
