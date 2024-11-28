import 'dart:io';

import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:just_the_tooltip/just_the_tooltip.dart';
import 'package:lite_agent_client/models/local_data_model.dart';

import 'logic.dart';

class AdjustmentPage extends StatelessWidget {
  AdjustmentPage({Key? key}) : super(key: key);

  final logic = Get.put(AdjustmentLogic());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        body: Column(
      children: [
        _buildTitleContainer(),
        Expanded(
            child: Container(
                color: const Color(0xffF5F5F5),
                padding: const EdgeInsets.all(10),
                child: Row(children: [
                  _buildLeftContainer(),
                  const SizedBox(width: 10),
                  Expanded(child: _buildChatContainer()),
                ])))
      ],
    ));
  }

  Container _buildChatContainer() {
    return Container(
        padding: const EdgeInsets.all(10),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.all(Radius.circular(8)),
        ),
        child: Column(
          children: [
            Row(
              children: [
                const Text("调试", style: TextStyle(fontSize: 16, color: Colors.black)),
                const Spacer(),
                InkWell(
                    onTap: () => logic.clearAllMessage(),
                    child: const Row(
                      children: [
                        Icon(Icons.dry_cleaning, size: 16, color: Colors.blue),
                        SizedBox(width: 5),
                        Text("清除", style: TextStyle(fontSize: 14, color: Colors.blue))
                      ],
                    )),
                const SizedBox(width: 15)
              ],
            ),
            Expanded(
                child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey),
                      borderRadius: const BorderRadius.all(Radius.circular(8)),
                    ),
                    child: Obx(() => ListView.builder(
                        controller: logic.chatScrollController,
                        itemCount: logic.chatMessageList.length,
                        itemBuilder: (context, index) {
                          return _buildMessageItem(logic.chatMessageList[index]);
                        })))),
            Container(
                height: 44,
                padding: const EdgeInsets.symmetric(horizontal: 10),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey),
                  borderRadius: const BorderRadius.all(Radius.circular(8)),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(
                        child: TextField(
                            onSubmitted: (string) {
                              logic.onSendButtonPress();
                            },
                            focusNode: logic.chatFocusNode,
                            controller: logic.chatController,
                            cursorColor: Colors.blue,
                            maxLines: 1,
                            decoration: const InputDecoration(
                                hintText: '请输入聊天内容',
                                border: InputBorder.none,
                                isDense: true,
                                contentPadding: EdgeInsets.symmetric(horizontal: 8)),
                            style: const TextStyle(fontSize: 14))),
                    IconButton(
                        onPressed: () {
                          logic.onSendButtonPress();
                        },
                        icon: const Icon(Icons.send, size: 24))
                  ],
                )),
          ],
        ));
  }

  Container _buildLeftContainer() {
    return Container(
        width: 360,
        padding: const EdgeInsets.all(10),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.all(Radius.circular(8)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("模型", style: TextStyle(fontSize: 16, color: Colors.black)),
            const SizedBox(height: 10),
            _buildModelSelectRow(),
            Container(
                margin: const EdgeInsets.symmetric(vertical: 10),
                child: const Text("系统提示词", style: TextStyle(fontSize: 16, color: Colors.black))),
            Container(
                height: 150,
                padding: const EdgeInsets.symmetric(vertical: 10),
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey),
                  borderRadius: BorderRadius.circular(2),
                ),
                child: TextField(
                    maxLines: null,
                    controller: logic.tipsController,
                    decoration: const InputDecoration(
                        hintText: '请输入系统提示词', border: InputBorder.none, isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 8)),
                    style: const TextStyle(fontSize: 14))),
            Container(
                margin: const EdgeInsets.symmetric(vertical: 10),
                child: Row(
                  children: [
                    const Text("工具", style: TextStyle(fontSize: 16, color: Colors.black)),
                    const SizedBox(width: 10),
                    JustTheTooltip(
                      tailBaseWidth: 16,
                      tailLength: 8,
                      content: Container(
                          padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 12),
                          decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
                          child: const Text("agent在特定场景下可以调用工具，可以更好执行指令", style: TextStyle(color: Colors.grey, fontSize: 14))),
                      child: const Icon(Icons.warning, size: 16, color: Colors.blue),
                    ),
                    const Spacer(),
                    InkWell(
                        onTap: () {
                          logic.showToolSelectDialog();
                        },
                        child: const Row(
                          children: [
                            Icon(Icons.add, size: 16, color: Colors.blue),
                            SizedBox(width: 5),
                            Text("添加", style: TextStyle(fontSize: 14, color: Colors.blue))
                          ],
                        )),
                    const SizedBox(width: 15)
                  ],
                )),
            Expanded(
              child: Obx(() => ListView.builder(
                    itemCount: logic.toolList.length,
                    itemBuilder: (context, index) => _buildToolItem(logic.toolList[index]),
                  )),
            )
          ],
        ));
  }

  Row _buildModelSelectRow() {
    return Row(children: [
      Expanded(
          child: Container(
              height: 32,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey),
                borderRadius: const BorderRadius.all(Radius.circular(4)),
              ),
              child: Center(child: Obx(() {
                /*if (logic.modelList.isEmpty) {
                  return Container();
                }*/
                var newButtonTag = "newButton";
                var list = <ModelBean>[];
                list.assignAll(logic.modelList);
                ModelBean newButton = ModelBean();
                newButton.id = newButtonTag;
                newButton.name = "新建模型";
                list.add(newButton);
                var selectId = logic.selectedModelId;
                return DropdownButtonHideUnderline(
                    child: DropdownButton2(
                  isExpanded: true,
                  items: list.map<DropdownMenuItem<String>>((ModelBean item) {
                    var textColor = item.id != newButtonTag ? Colors.black : Colors.blue;
                    return DropdownMenuItem<String>(
                        value: item.id, child: Text(item.name, style: TextStyle(fontSize: 14, color: textColor)));
                  }).toList(),
                  value: selectId.isEmpty ? null : selectId,
                  onChanged: (value) {
                    if (value != null && value != newButtonTag) {
                      logic.selectModel(value);
                      selectId = value;
                    } else {
                      logic.showCreateModelDialog();
                    }
                  },
                  dropdownStyleData:
                      const DropdownStyleData(offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
                ));
              })))),
      const SizedBox(width: 10),
      Container(
        width: 80,
        height: 32,
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey),
          borderRadius: const BorderRadius.all(Radius.circular(4)),
        ),
        child: DropdownButtonHideUnderline(
            child: DropdownButton2(
                customButton: const Center(child: Text('更多', style: TextStyle(color: Colors.black, fontSize: 14))),
                isExpanded: true,
                dropdownStyleData: DropdownStyleData(
                    width: 360,
                    offset: const Offset(0, -10),
                    padding: const EdgeInsets.symmetric(vertical: 0),
                    decoration: BoxDecoration(
                        border: Border.all(color: Colors.grey),
                        borderRadius: const BorderRadius.all(Radius.circular(4)),
                        color: Colors.white)),
                menuItemStyleData: MenuItemStyleData(
                    height: 150, overlayColor: WidgetStateProperty.resolveWith<Color>((Set<WidgetState> states) => Colors.transparent)),
                items: [_buildParamsPopItem()],
                onChanged: (value) {})),
      )
    ]);
  }

  DropdownMenuItem<String> _buildParamsPopItem() {
    return DropdownMenuItem<String>(
        child: Column(
      children: [
        Obx(() => _buildSliderRow("temperature", 1.0, true, logic.sliderTempValue)),
        Obx(() => _buildSliderRow("maxToken", 4096, false, logic.sliderTokenValue)),
        Obx(() => _buildSliderRow("topP", 1.0, true, logic.sliderTopPValue)),
      ],
    ));
  }

  Row _buildSliderRow(String title, double maxValue, bool needDecimal, Rx<double> sliderValue) {
    String outputValueString = needDecimal ? sliderValue.value.toStringAsFixed(1).toString() : sliderValue.value.toInt().toString();
    return Row(
      children: [
        SizedBox(
          width: 75,
          child: Text(title, style: const TextStyle(fontSize: 12)),
        ),
        Expanded(
            child: Slider(
                value: sliderValue.value,
                min: 0.0,
                max: maxValue,
                activeColor: Colors.blue,
                inactiveColor: Colors.grey,
                thumbColor: Colors.blue,
                onChanged: (double value) {
                  sliderValue.value = value;
                })),
        Container(
            width: 48,
            height: 26,
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey),
              borderRadius: const BorderRadius.all(Radius.circular(8)),
            ),
            child: Center(
              child: Text(outputValueString, style: const TextStyle(fontSize: 12)),
            ))
      ],
    );
  }

  Widget _buildToolItem(ToolBean tool) {
    return MouseRegion(
        onEnter: (event) {
          logic.toolHoverItemId.value = tool.id;
        },
        onExit: (event) {
          logic.toolHoverItemId.value = "";
        },
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Obx(() {
            var isSelect = logic.toolHoverItemId.value == tool.id;
            var backgroundColor = isSelect ? Colors.black12 : Colors.transparent;
            return Container(
                padding: const EdgeInsets.all(10),
                color: backgroundColor,
                child: Row(children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: const BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.all(Radius.circular(8))),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(tool.name,
                        style: const TextStyle(fontSize: 14, color: Colors.black), maxLines: 1, overflow: TextOverflow.ellipsis),
                    Text(tool.description,
                        style: const TextStyle(fontSize: 14, color: Colors.grey), maxLines: 1, overflow: TextOverflow.ellipsis)
                  ])),
                  if (!isSelect)
                    Container()
                  else
                    Row(children: [
                      InkWell(
                          onTap: () {
                            logic.showEditToolDialog(tool);
                          },
                          child: const Icon(Icons.settings, size: 24, color: Colors.grey)),
                      const SizedBox(width: 10),
                      InkWell(
                          onTap: () {
                            logic.removeTool(tool.id);
                          },
                          child: const Icon(Icons.delete, size: 24, color: Colors.grey)),
                    ])
                ]));
          }),
          const Divider(height: 0.5, color: Colors.grey)
        ]));
  }

  Container _buildTitleContainer() {
    return Container(
        color: const Color(0xFF001528),
        height: 60,
        child: Row(children: [
          Obx(() => Offstage(
                offstage: logic.isFullScreen.value,
                child: const SizedBox(width: 50),
              )),
          Container(
              margin: const EdgeInsets.only(left: 10, right: 5),
              child: InkWell(
                  onTap: () {
                    logic.goBack();
                  },
                  child: Container(padding: const EdgeInsets.all(13), child: const Icon(Icons.arrow_back, size: 24, color: Colors.white)))),
          Obx(() {
            var iconPath = logic.agent.value?.iconPath ?? "";
            var name = logic.agent.value?.name ?? "";
            return Row(
              children: [
                if (iconPath.isNotEmpty)
                  Image(image: FileImage(File(iconPath)), height: 38, width: 38, fit: BoxFit.cover)
                else
                  Container(width: 38, height: 38, decoration: BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.circular(6))),
                const SizedBox(width: 10),
                Text(name, style: const TextStyle(fontSize: 14, color: Colors.white))
              ],
            );
          }),
          const SizedBox(width: 10),
          InkWell(onTap: () => logic.showEditAgentDialog(), child: const Icon(Icons.edit, size: 16, color: Colors.white)),
          const Spacer(),
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 20)),
                  backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                    RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                  )),
              onPressed: () => logic.backToChat(),
              child: const Text('进入聊天', style: TextStyle(color: Colors.white, fontSize: 14))),
          const SizedBox(width: 20),
          InkWell(
              onTap: () => logic.updateAgentInfo(),
              child: Container(
                  padding: const EdgeInsets.all(10),
                  child: const Center(child: Text("保存", style: TextStyle(fontSize: 14, color: Colors.white))))),
          const SizedBox(width: 10),
          DropdownButtonHideUnderline(
              child: DropdownButton2(
                  customButton: const Text("更多", style: TextStyle(fontSize: 14, color: Colors.white)),
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
                      var agentId = logic.agent.value?.id ?? "";
                      logic.removeAgent(agentId);
                    }
                  })),
          const SizedBox(width: 30)
        ]));
  }

  Widget _buildMessageItem(ChatMessage chatMessage) {
    if (chatMessage.sendRole == ChatRole.User) {
      return Container(
        margin: const EdgeInsets.fromLTRB(15, 15, 15, 0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Row(children: [
                  //Text(chatMessage.userName, style: const TextStyle(fontSize: 14, color: Colors.black)),
                  Container(
                      width: 25,
                      height: 25,
                      decoration: BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.circular(6)),
                      margin: const EdgeInsets.fromLTRB(5, 0, 0, 5))
                ]),
                Text(chatMessage.message,
                    textAlign: TextAlign.right, overflow: TextOverflow.clip, style: const TextStyle(fontSize: 14, color: Colors.black))
              ],
            )
          ],
        ),
      );
    } else if (chatMessage.sendRole == ChatRole.Agent) {
      var name = logic.agent.value?.name ?? "Agent";
      return Container(
        margin: const EdgeInsets.fromLTRB(15, 15, 15, 0),
        child: Row(
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(children: [
                  Container(
                      width: 25,
                      height: 25,
                      decoration: BoxDecoration(color: Colors.grey, borderRadius: BorderRadius.circular(6)),
                      margin: const EdgeInsets.fromLTRB(0, 0, 5, 5))
                  //Text(name, style: const TextStyle(fontSize: 14, color: Colors.black))
                ]),
                Text(chatMessage.message, overflow: TextOverflow.clip, style: const TextStyle(fontSize: 14, color: Colors.black))
              ],
            )
          ],
        ),
      );
    } else {
      return Container();
    }
  }
}
