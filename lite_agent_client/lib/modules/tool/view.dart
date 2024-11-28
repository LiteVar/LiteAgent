import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/tool.dart';

import 'logic.dart';

class ToolPage extends StatelessWidget {
  ToolPage({Key? key}) : super(key: key);

  final logic = Get.put(ToolLogic());

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
            _buildTitle(),
            Expanded(
              child: Stack(
                children: [
                  Column(children: [
                    _buildSecondaryTitle(),
                    const SizedBox(height: 10),
                    buildListExpanded(),
                  ]),
                  buildLoginCover()
                ],
              ),
            )
          ],
        ),
      ),
    );
  }

  Widget buildLoginCover() {
    return Obx(() => Offstage(
          offstage: logic.currentTab.value == ToolLogic.TAB_LOCAL || logic.isLogin,
          child: Container(
            color: Colors.white,
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    width: 79,
                    height: 79,
                    color: itemBorderColor,
                  ),
                  const SizedBox(height: 20),
                  const Text("您需要登录后才可查看同步云端信息", style: TextStyle(fontSize: 12)),
                  const SizedBox(height: 20),
                  TextButton(
                      style: ButtonStyle(
                          padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(24, 18, 24, 18)),
                          backgroundColor: WidgetStateProperty.all(buttonColor),
                          shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                            RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(10),
                            ),
                          )),
                      onPressed: () {
                        logic.onLoginButtonClick();
                      },
                      child: const Text('登录', style: TextStyle(color: Colors.white, fontSize: 14)))
                ],
              ),
            ),
          ),
        ));
  }

  Widget buildListExpanded() {
    return Expanded(child: Obx(() {
      return GridView.count(
        crossAxisCount: 4,
        childAspectRatio: 3 / 2.2,
        children: List.generate(logic.currentToolList.length, (index) => _buildToolItem(logic.currentToolList[index])),
      );
    }));
  }

  Widget _buildTitle() {
    return Row(children: [
      Obx(() {
        var localColor = logic.currentTab.value == ToolLogic.TAB_LOCAL ? Colors.black : Colors.grey;
        var cloudColor = logic.currentTab.value == ToolLogic.TAB_CLOUD ? Colors.black : Colors.grey;
        return Row(children: [
          TextButton(
              onPressed: () {
                logic.switchTab(ToolLogic.TAB_LOCAL);
              },
              child: Text('本地工具管理', style: TextStyle(fontSize: 18, color: localColor))),
          TextButton(
              onPressed: () {
                logic.switchTab(ToolLogic.TAB_CLOUD);
              },
              child: Text('云端工具管理', style: TextStyle(fontSize: 18, color: cloudColor)))
        ]);
      }),
      const Spacer(),
      _buildNewToolButton(),
    ]);
  }

  Widget _buildSecondaryTitle() {
    return Obx(() {
      String tab = logic.currentSecondaryTab.value;
      var allColor = tab == ToolLogic.TAB_SEC_ALL ? Colors.black : Colors.grey;
      var sysColor = tab == ToolLogic.TAB_SEC_SYSTEM ? Colors.black : Colors.grey;
      var shareColor = tab == ToolLogic.TAB_SEC_SHARE ? Colors.black : Colors.grey;
      var meColor = tab == ToolLogic.TAB_SEC_MINE ? Colors.black : Colors.grey;
      return Offstage(
        offstage: logic.currentTab.value == ToolLogic.TAB_LOCAL,
        child: Container(
            margin: const EdgeInsets.only(top: 30),
            child: Row(
              children: [
                TextButton(
                    onPressed: () {
                      logic.switchTab(ToolLogic.TAB_SEC_ALL);
                    },
                    child: Text('全部', style: TextStyle(fontSize: 16, color: allColor))),
                TextButton(
                    onPressed: () {
                      logic.switchTab(ToolLogic.TAB_SEC_SYSTEM);
                    },
                    child: Text('系统', style: TextStyle(fontSize: 16, color: sysColor))),
                TextButton(
                    onPressed: () {
                      logic.switchTab(ToolLogic.TAB_SEC_SHARE);
                    },
                    child: Text('分享', style: TextStyle(fontSize: 16, color: shareColor))),
                TextButton(
                    onPressed: () {
                      logic.switchTab(ToolLogic.TAB_SEC_MINE);
                    },
                    child: Text('我的', style: TextStyle(fontSize: 16, color: meColor)))
              ],
            )),
      );
    });
  }

  Widget _buildNewToolButton() {
    return TextButton(
        style: ButtonStyle(
            padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(24, 18, 24, 18)),
            backgroundColor: WidgetStateProperty.all(buttonColor),
            shape: WidgetStateProperty.all<RoundedRectangleBorder>(
              RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
            )),
        onPressed: () {
          logic.showCreateToolDialog();
        },
        child: const Text('新建本地工具', style: TextStyle(color: Colors.white, fontSize: 14)));
  }

  Widget _buildToolItem(ToolDTO tool) {
    return Container(
      margin: const EdgeInsets.all(10),
      padding: const EdgeInsets.fromLTRB(15, 10, 15, 10),
      decoration: BoxDecoration(
        border: Border.all(color: itemBorderColor),
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.android, color: Colors.grey, size: 30),
              const SizedBox(width: 16),
              Expanded(
                child: Text(
                  tool.name ?? "",
                  style: const TextStyle(fontSize: 16, color: Colors.black),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Offstage(
                offstage: !(tool.shareFlag ?? false),
                child: Container(
                  width: 44,
                  height: 24,
                  margin: const EdgeInsets.only(left: 4),
                  decoration: BoxDecoration(
                    color: const Color.fromRGBO(255, 195, 0, 1),
                    borderRadius: BorderRadius.circular(8.0),
                  ),
                  child: const Center(
                    child: Text("已分享", style: TextStyle(fontSize: 10, color: Colors.white)),
                  ),
                ),
              )
            ],
          ),
          const SizedBox(height: 4),
          SizedBox(
              height: 68,
              child: Text(
                tool.description ?? "",
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 14),
              )),
          const Spacer(),
          Obx(() {
            if (logic.currentTab.value == ToolLogic.TAB_LOCAL) {
              return Container(
                  margin: const EdgeInsets.symmetric(horizontal: 10),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      InkWell(
                        onTap: () {
                          logic.showEditToolDialog(tool.id);
                        },
                        child: const Row(
                          children: [
                            Icon(Icons.newspaper, color: Colors.blue, size: 16),
                            SizedBox(width: 4),
                            Text('编辑', style: TextStyle(color: Colors.blue, fontSize: 14))
                          ],
                        ),
                      ),
                      DropdownButtonHideUnderline(
                          child: DropdownButton2(
                              customButton: const Row(
                                children: [
                                  Icon(Icons.more, color: Colors.blue, size: 16),
                                  SizedBox(width: 4),
                                  Text('更多', style: TextStyle(color: Colors.blue, fontSize: 14)),
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
                                    child: Center(
                                      child: Text("删除", style: TextStyle(fontSize: 14)),
                                    ))
                              ],
                              onChanged: (value) {
                                if (value == "delete") {
                                  logic.removeTool(tool.id);
                                }
                              })),
                    ],
                  ));
            } else {
              return InkWell(
                  onTap: () {
                    logic.showToolDetailDialog(tool.id);
                  },
                  child: const Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                    Icon(Icons.newspaper, color: Colors.blue, size: 16),
                    SizedBox(width: 4),
                    Text('查看详情', style: TextStyle(color: Colors.blue, fontSize: 14))
                  ]));
            }
          })
        ],
      ),
    );
  }
}
