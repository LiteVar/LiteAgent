import 'dart:io';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';

import '../../models/local_data_model.dart';
import 'dialog_tool_edit.dart';

class SelectToolDialog extends StatelessWidget {
  var selectIdList = <String>[];
  final void Function(List<ToolBean> tools) onConfirm;

  SelectToolDialog({super.key, required this.selectIdList, required this.onConfirm});

  static const String TAB_ALL = "all";
  static const String TAB_SYSTEM = "system";
  static const String TAB_SHARE = "share";
  static const String TAB_MINE = "mine";

  var currentTab = TAB_ALL.obs;
  var toolList = <ToolBean>[].obs;

  Future<void> initData() async {
    toolList.assignAll((await toolRepository.getToolListFromBox()));
  }

  void switchSecondaryTab(String tabType) {
    currentTab.value = tabType;
  }

  void _selectItem(String id) {
    selectIdList.add(id);
    toolList.refresh();
  }

  void _removeItem(String id) {
    selectIdList.remove(id);
    toolList.refresh();
  }

  void _onClose() {
    var list = <ToolBean>[];
    for (var id in selectIdList) {
      for (var tool in toolList) {
        if (id == tool.id) {
          list.add(tool);
          break;
        }
      }
    }
    onConfirm(list);
    Get.back();
  }

  @override
  Widget build(BuildContext context) {
    initData();
    return Center(
        child: Container(
            width: 586,
            height: 560,
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.all(Radius.circular(6)),
            ),
            child: Column(children: [
              buildTitleContainer(),
              const SizedBox(height: 10),
              _buildTabContainer(),
              Expanded(child: _buildAgentListView()),
              const SizedBox(height: 10)
              //_buildAgentListView()
            ])));
  }

  /*Widget _buildTabContainer() {
    return Obx(() {
      var allColor = currentTab == TAB_ALL ? Colors.black : Colors.grey;
      var sysColor = currentTab == TAB_SYSTEM ? Colors.black : Colors.grey;
      var shareColor = currentTab == TAB_SHARE ? Colors.black : Colors.grey;
      var meColor = currentTab == TAB_MINE ? Colors.black : Colors.grey;
      return Container(
        margin: EdgeInsets.symmetric(vertical: 10),
        child: Row(children: [
          TextButton(
              onPressed: () {
                switchSecondaryTab(TAB_ALL);
              },
              child: Text('全部', style: TextStyle(fontSize: 16, color: allColor))),
          TextButton(
              onPressed: () {
                switchSecondaryTab(TAB_SYSTEM);
              },
              child: Text('系统', style: TextStyle(fontSize: 16, color: sysColor))),
          TextButton(
              onPressed: () {
                switchSecondaryTab(TAB_SHARE);
              },
              child: Text('分享', style: TextStyle(fontSize: 16, color: shareColor))),
          TextButton(
              onPressed: () {
                switchSecondaryTab(TAB_MINE);
              },
              child: Text('我的', style: TextStyle(fontSize: 16, color: meColor))),
          Spacer(),
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 15)),
                  backgroundColor: WidgetStateProperty.all(Color(0xFF2a82f5)),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                    RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(4.0),
                    ),
                  )),
              onPressed: () {
                _showEditToolDialog(null);
              },
              child: Text("新建工具", style: TextStyle(color: Colors.white, fontSize: 14))),
          SizedBox(width: 20)
        ]),
      );
    });
  }*/

  Widget _buildTabContainer() {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Row(children: [
        const Spacer(),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 15)),
                backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                )),
            onPressed: () {
              _showCreateToolDialog();
            },
            child: const Text("新建工具", style: TextStyle(color: Colors.white, fontSize: 14))),
        const SizedBox(width: 20)
      ]),
    );
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        const Text("添加工具"),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () {
            _onClose();
          },
        )
      ]),
    );
  }

  Widget _buildAgentListView() {
    return Obx(() {
      return ListView.builder(
          itemCount: toolList.length,
          itemBuilder: (context, index) {
            var tool = toolList[index];
            var isSelected = false;
            for (var id in selectIdList) {
              if (id == tool.id) {
                isSelected = true;
                break;
              }
            }
            String buttonText = isSelected ? "移除" : "添加";
            return Container(
              padding: const EdgeInsets.all(15),
              margin: const EdgeInsets.fromLTRB(10, 10, 10, 0),
              decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey, width: 0.5), borderRadius: const BorderRadius.all(Radius.circular(8))),
              child: Row(
                children: [
                  Expanded(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Container(
                          width: 29,
                          height: 29,
                          decoration: const BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.all(Radius.circular(8)))),
                      Container(
                          margin: const EdgeInsets.fromLTRB(15, 4, 15, 0),
                          child: Text(tool.name, style: const TextStyle(fontSize: 16, color: Colors.black)))
                    ]),
                    Container(
                        margin: const EdgeInsets.only(top: 10),
                        child: Text(tool.description,
                            maxLines: 3, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Colors.grey))),
                  ])),
                  TextButton(
                      style: ButtonStyle(
                          padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 15)),
                          backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                          shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                              RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)))),
                      onPressed: () {
                        if (isSelected) {
                          _removeItem(tool.id);
                        } else {
                          _selectItem(tool.id);
                        }
                      },
                      child: Text(buttonText, style: const TextStyle(color: Colors.white, fontSize: 14)))
                ],
              ),
            );
          });
    });
  }

  void _showCreateToolDialog() {
    Get.dialog(
        barrierDismissible: false,
        EditToolDialog(
            tool: null,
            isEdit: false,
            onConfirmCallback: (String name, String description, String schemaType, String schemaText, String apiType, String apiText) {
              _updateTool("", name, description, schemaType, schemaText, apiType, apiText);
            }));
  }

  void _updateTool(String id, String name, String description, String schemaType, String schemaText, String apiType, String apiText) {
    ToolBean targetTool = ToolBean();
    targetTool.id = DateTime.now().microsecondsSinceEpoch.toString();
    toolList.add(targetTool);

    targetTool.name = name;
    targetTool.description = description;
    targetTool.schemaText = schemaText;
    targetTool.schemaType = schemaType;
    targetTool.apiText = apiText;
    targetTool.apiType = apiType;
    toolList.refresh();
    toolRepository.updateTool(targetTool.id, targetTool);
    eventBus.fire(ToolMessageEvent(message: EventBusMessage.updateList));
  }
}
