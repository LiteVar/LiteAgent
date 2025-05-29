import 'dart:io';

import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:get/get.dart';
import 'package:just_the_tooltip/just_the_tooltip.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../utils/web_util.dart';
import 'logic.dart';

class AdjustmentPage extends StatelessWidget {
  AdjustmentPage({Key? key}) : super(key: key);

  final logic = Get.put(AdjustmentLogic());

  @override
  Widget build(BuildContext context) {
    return Container(
        color: Colors.white,
        child: Column(children: [
          buildTitleContainer(),
          horizontalLine(),
          Expanded(
            child: Row(children: [
              buildLeftSettingContainer(),
              verticalLine(),
              Expanded(child: buildRightChatContainer()),
            ]),
          )
        ]));
  }

  Widget buildRightChatContainer() {
    return Column(
      children: [
        Container(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            margin: const EdgeInsets.only(top: 20),
            child: Row(children: [
              const Text("调试", style: TextStyle(fontSize: 18, color: Color(0xff333333))),
              const Spacer(),
              buildClickButton("icon_clear.png", "清除", () => logic.clearAllMessage()),
            ])),
        Expanded(
            child: Container(
                padding: const EdgeInsets.symmetric(vertical: 20),
                child: Obx(() => ListView.builder(
                    controller: logic.chatScrollController,
                    itemCount: logic.chatMessageList.length,
                    itemBuilder: (context, index) => _buildMessageItem(index, logic.chatMessageList[index]))))),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 20),
          margin: const EdgeInsets.only(bottom: 20),
          child: Container(
              height: 44,
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(8))),
              child: Obx(() => Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Expanded(
                          child: TextField(
                              enabled: logic.enableInput.value,
                              onSubmitted: (string) => logic.onSendButtonPress(),
                              focusNode: logic.chatFocusNode,
                              controller: logic.chatController,
                              cursorColor: const Color(0xff2A82E4),
                              maxLines: 1,
                              decoration: InputDecoration(
                                  hintText: logic.enableInput.value ? '请输入聊天内容' : '反思Agent不能进行聊天对话',
                                  border: InputBorder.none,
                                  isDense: true,
                                  contentPadding: const EdgeInsets.symmetric(horizontal: 8)),
                              style: const TextStyle(fontSize: 14))),
                      InkWell(
                          onTap: () => logic.enableInput.value ? logic.onSendButtonPress() : null,
                          child: Container(
                              margin: const EdgeInsets.only(right: 4),
                              padding: const EdgeInsets.all(4),
                              child: buildAssetImage("icon_send.png", 20, const Color(0xffb3b3b3))))
                    ],
                  ))),
        ),
      ],
    );
  }

  Widget buildLeftSettingContainer() {
    return SingleChildScrollView(
      child: Container(
        width: 360,
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("模型", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
            _buildModelSelectWidget(),
            Container(
                margin: const EdgeInsets.only(top: 10),
                child: const Text("系统提示词", style: TextStyle(fontSize: 14, color: Color(0xff333333)))),
            buildInputPromptContainer(),
            buildToolOptionColumn(),
            horizontalLine(),
            buildLibraryOptionColumn(),
            horizontalLine(),
            buildAgentOptionColumn(),
            buildChildAgentContainer(),
          ],
        ),
      ),
    );
  }

  Column buildAgentOptionColumn() {
    return Column(children: [
      const SizedBox(height: 10),
      Row(
        children: [
          const Text("Agent类型", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
          const Spacer(),
          DropdownButtonHideUnderline(
              child: DropdownButton2(
                  customButton: Container(
                      width: 240,
                      height: 32,
                      margin: const EdgeInsets.only(top: 5),
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(4))),
                      child: Obx(() => Row(
                            children: [
                              if (logic.agentType.value == AgentType.GENERAL)
                                const Text("普通", style: TextStyle(fontSize: 12, color: Color(0xff333333)))
                              else if (logic.agentType.value == AgentType.DISTRIBUTE)
                                const Text("分发", style: TextStyle(fontSize: 12, color: Color(0xff333333)))
                              else if (logic.agentType.value == AgentType.REFLECTION)
                                const Text("反思", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
                              const Spacer(),
                              buildAssetImage("icon_down.png", 12, const Color(0xff333333))
                            ],
                          ))),
                  dropdownStyleData: const DropdownStyleData(
                      width: 240,
                      offset: Offset(-0, -8),
                      padding: EdgeInsets.symmetric(vertical: 0),
                      decoration: BoxDecoration(color: Colors.white)),
                  menuItemStyleData: const MenuItemStyleData(height: 40),
                  items: const [
                    DropdownMenuItem<String>(value: "GENERAL", child: Text("普通", style: TextStyle(fontSize: 13))),
                    //DropdownMenuItem<String>(value: "DISTRIBUTE", child: Text("分发", style: TextStyle(fontSize: 13))),
                    DropdownMenuItem<String>(value: "REFLECTION", child: Text("反思", style: TextStyle(fontSize: 13))),
                  ],
                  onChanged: (value) => logic.setAgentType(value))),
        ],
      ),
      const SizedBox(height: 4),
      Row(
        children: [
          const Text("执行模式", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
          const Spacer(),
          DropdownButtonHideUnderline(
              child: DropdownButton2(
                  customButton: Container(
                      width: 240,
                      height: 32,
                      margin: const EdgeInsets.only(top: 5),
                      padding: const EdgeInsets.symmetric(horizontal: 12),
                      decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(4))),
                      child: Obx(() => Row(
                            children: [
                              if (logic.operationMode.value == OperationMode.PARALLEL)
                                const Text("并行", style: TextStyle(fontSize: 12, color: Color(0xff333333)))
                              else if (logic.operationMode.value == OperationMode.SERIAL)
                                const Text("串行", style: TextStyle(fontSize: 12, color: Color(0xff333333)))
                              else if (logic.operationMode.value == OperationMode.REJECT)
                                const Text("拒绝", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
                              const Spacer(),
                              buildAssetImage("icon_down.png", 12, const Color(0xff333333))
                            ],
                          ))),
                  dropdownStyleData: const DropdownStyleData(
                      width: 240,
                      offset: Offset(-0, -8),
                      padding: EdgeInsets.symmetric(vertical: 0),
                      decoration: BoxDecoration(color: Colors.white)),
                  menuItemStyleData: const MenuItemStyleData(height: 40),
                  items: const [
                    DropdownMenuItem<String>(value: "parallel", child: Text("并行", style: TextStyle(fontSize: 13))),
                    DropdownMenuItem<String>(value: "serial", child: Text("串行", style: TextStyle(fontSize: 13))),
                    DropdownMenuItem<String>(value: "reject", child: Text("拒绝", style: TextStyle(fontSize: 13))),
                  ],
                  onChanged: (value) {
                    if (value == "parallel") {
                      logic.operationMode.value = OperationMode.PARALLEL;
                    } else if (value == "serial") {
                      logic.operationMode.value = OperationMode.SERIAL;
                    } else if (value == "reject") {
                      logic.operationMode.value = OperationMode.REJECT;
                    }
                  })),
        ],
      ),
    ]);
  }

  Container buildChildAgentContainer() {
    return Container(
        margin: const EdgeInsets.only(top: 10, bottom: 80),
        child: Row(children: [
          const Text("子Agent", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
          const Spacer(),
          buildClickButton("icon_setting.png", "设置", () => logic.showChildAgentSelectDialog()),
        ]));
  }

  Column buildToolOptionColumn() {
    return Column(
      children: [
        Container(
            margin: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                const Text("工具", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
                const SizedBox(width: 4),
                JustTheTooltip(
                  tailBaseWidth: 16,
                  tailLength: 8,
                  content: Container(
                      padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 12),
                      decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
                      child: const Text("agent在特定场景下可以调用工具，可以更好执行指令", style: TextStyle(color: Color(0xff999999), fontSize: 14))),
                  child: buildAssetImage("icon_notified.png", 14, const Color(0xff333333)),
                ),
                const Spacer(),
                //buildClickButton("icon_set.png", "设置", () => logic.showToolSelectDialog()),
                const SizedBox(width: 10),
                buildClickButton("icon_add.png", "添加", () => logic.showToolFunctionSelectDialog()),
                const SizedBox(width: 10),
                buildToolSettingWidget()
              ],
            )),
        Obx(() => Container(
            margin: const EdgeInsets.only(top: 10),
            child: Column(children: [
              if (logic.functionList.isNotEmpty)
                ...List.generate(
                  !logic.showMoreTool.value && logic.functionList.length > 2 ? 2 : logic.functionList.length,
                  (index) => _buildToolFunctionItem(index, logic.functionList[index]),
                )
            ]))),
        Obx(() => Offstage(
              offstage: logic.functionList.length <= 2,
              child: InkWell(
                onTap: () => logic.showMoreTool.value = !logic.showMoreTool.value,
                child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(logic.showMoreTool.value ? "收起" : "更多", style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                        const SizedBox(width: 10),
                        buildAssetImage(logic.showMoreTool.value ? "icon_up.png" : "icon_down.png", 12, const Color(0xff2A82E4))
                      ],
                    )),
              ),
            ))
      ],
    );
  }

  Widget buildToolSettingWidget() {
    return DropdownButtonHideUnderline(
        child: DropdownButton2(
            customButton: buildClickButton("icon_down.png", "更多", null),
            isExpanded: true,
            dropdownStyleData: const DropdownStyleData(
                width: 130, offset: Offset(-10, -8), padding: EdgeInsets.all(0), decoration: BoxDecoration(color: Colors.white)),
            menuItemStyleData: MenuItemStyleData(
                height: 40,
                padding: const EdgeInsets.all(0),
                overlayColor: WidgetStateProperty.resolveWith<Color>((Set<WidgetState> states) => Colors.transparent)),
            items: [buildToolOperationModeMenuItem()],
            onChanged: (value) {}));
  }

  DropdownMenuItem<String> buildToolOperationModeMenuItem() {
    return DropdownMenuItem<String>(
        value: "operationMode",
        child: Obx(() => DropdownButtonHideUnderline(
            child: DropdownButton2(
                customButton: SizedBox(
                  height: 40,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text("工具执行模式", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                      const SizedBox(width: 5),
                      buildAssetImage("icon_right.png", 12, const Color(0xff2A82E4))
                    ],
                  ),
                ),
                dropdownStyleData: const DropdownStyleData(
                    width: 80, offset: Offset(135, 40), padding: EdgeInsets.all(0), decoration: BoxDecoration(color: Colors.white)),
                menuItemStyleData: const MenuItemStyleData(
                  height: 40,
                ),
                items: [
                  DropdownMenuItem<String>(
                      value: "parallel",
                      child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                        Visibility(
                            visible: logic.toolOperationMode.value == OperationMode.PARALLEL,
                            child: buildAssetImage("icon_checked.png", 12, const Color(0xff2A82E4))),
                        const SizedBox(width: 5),
                        const Text("并行", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
                      ])),
                  DropdownMenuItem<String>(
                      value: "serial",
                      child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                        Visibility(
                            visible: logic.toolOperationMode.value == OperationMode.SERIAL,
                            child: buildAssetImage("icon_checked.png", 12, const Color(0xff2A82E4))),
                        const SizedBox(width: 5),
                        const Text("串行", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
                      ])),
                  DropdownMenuItem<String>(
                      value: "reject",
                      child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                        Visibility(
                            visible: logic.toolOperationMode.value == OperationMode.REJECT,
                            child: buildAssetImage("icon_checked.png", 12, const Color(0xff2A82E4))),
                        const SizedBox(width: 5),
                        const Text("拒绝", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
                      ])),
                ],
                onChanged: (value) {
                  if (value == "parallel") {
                    logic.toolOperationMode.value = OperationMode.PARALLEL;
                  } else if (value == "serial") {
                    logic.toolOperationMode.value = OperationMode.SERIAL;
                  } else if (value == "reject") {
                    logic.toolOperationMode.value = OperationMode.REJECT;
                  }
                }))));
  }

  Column buildLibraryOptionColumn() {
    return Column(
      children: [
        Container(
            margin: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                const Text("知识库", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
                const Spacer(),
                buildClickButton("icon_add.png", "添加", () => logic.showLibrarySelectDialog()),
              ],
            )),
        Obx(() => Container(
            margin: const EdgeInsets.only(top: 10),
            child: Column(children: [
              if (logic.libraryList.isNotEmpty)
                ...List.generate(
                  !logic.showMoreLibrary.value && logic.libraryList.length > 2 ? 2 : logic.libraryList.length,
                  (index) => _buildLibraryItem(index, logic.libraryList[index]),
                )
            ]))),
        Obx(() => Offstage(
              offstage: logic.libraryList.length <= 2,
              child: InkWell(
                onTap: () => logic.showMoreLibrary.value = !logic.showMoreLibrary.value,
                child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(logic.showMoreLibrary.value ? "收起" : "更多", style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                        const SizedBox(width: 10),
                        buildAssetImage("icon_down.png", 12, const Color(0xff2A82E4))
                      ],
                    )),
              ),
            ))
      ],
    );
  }

  InkWell buildClickButton(String fileName, String text, Function()? onTap) {
    return InkWell(
        onTap: onTap,
        child: Container(
            padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 12),
            decoration: const BoxDecoration(color: Color(0xffe7f2fe), borderRadius: BorderRadius.all(Radius.circular(8))),
            child: Row(
              children: [
                buildAssetImage(fileName, 14, const Color(0xff2A82E4)),
                const SizedBox(width: 5),
                Text(text, style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
              ],
            )));
  }

  Widget buildInputPromptContainer() {
    return Stack(children: [
      Obx(() => Container(
          height: logic.inputPromptHeight.value,
          margin: const EdgeInsets.symmetric(vertical: 10),
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(4)),
          child: TextField(
              maxLines: null,
              controller: logic.tipsController,
              decoration: const InputDecoration(
                  hintText: '请输入系统提示词', border: InputBorder.none, isDense: true, contentPadding: EdgeInsets.symmetric(horizontal: 10)),
              style: const TextStyle(fontSize: 14, color: Color(0xff333333))))),
      Positioned(
        right: 0,
        bottom: 10,
        child: GestureDetector(
            onVerticalDragUpdate: (details) => logic.updateHeight(details.delta.dy),
            child: const Icon(Icons.signal_cellular_4_bar, size: 16, color: Color(0xfff999999))),
      )
    ]);
  }

  Widget _buildModelSelectWidget() {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Row(children: [
        Expanded(
            child: Container(
                height: 36,
                decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(4))),
                child: Center(child: Obx(
                  () {
                    var newButtonTag = "newButton";
                    var list = <ModelBean>[];
                    list.assignAll(logic.modelList);
                    ModelBean newButton = ModelBean();
                    newButton.id = newButtonTag;
                    newButton.name = "新建模型";
                    list.add(newButton);
                    var selectId = logic.currentModel?.id ?? "";
                    return DropdownButtonHideUnderline(
                        child: DropdownButton2(
                      isExpanded: true,
                      items: list.map<DropdownMenuItem<String>>((ModelBean item) {
                        var textColor = item.id != newButtonTag ? const Color(0xff333333) : const Color(0xff2A82E4);
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
                  },
                )))),
        const SizedBox(width: 10),
        DropdownButtonHideUnderline(
            child: DropdownButton2(
                customButton: buildCommonTextButton("更多", 32, 16, null),
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
      ]),
    );
  }

  DropdownMenuItem<String> _buildParamsPopItem() {
    return DropdownMenuItem<String>(
        child: Column(
      children: [
        Obx(() => _buildSliderRow("temperature", 1.0, 0.0, true, logic.sliderTempValue)),
        Obx(() {
          String maxTokenString = logic.currentModel?.maxToken ?? "4096";
          int maxTokenLimit = int.parse(maxTokenString);
          return _buildSliderRow("maxToken", maxTokenLimit.toDouble(), 1, false, logic.sliderTokenValue);
        }),
        Obx(() => _buildSliderRow("topP", 1.0, 0.0, true, logic.sliderTopPValue)),
      ],
    ));
  }

  Row _buildSliderRow(String title, double maxValue, double minValue, bool needDecimal, Rx<double> sliderValue) {
    String outputValueString = needDecimal ? sliderValue.value.toStringAsFixed(1).toString() : sliderValue.value.toInt().toString();
    return Row(
      children: [
        SizedBox(width: 75, child: Text(title, style: const TextStyle(fontSize: 12, color: Color(0xff333333)))),
        Expanded(
            child: Slider(
                value: sliderValue.value,
                min: minValue,
                max: maxValue,
                activeColor: const Color(0xff2A82E4),
                inactiveColor: const Color(0xff999999),
                thumbColor: const Color(0xff2A82E4),
                onChanged: (double value) {
                  sliderValue.value = value;
                })),
        Container(
            width: 48,
            height: 26,
            decoration:
                BoxDecoration(border: Border.all(color: const Color(0xff333333)), borderRadius: const BorderRadius.all(Radius.circular(4))),
            child: Center(
              child: Text(outputValueString, style: const TextStyle(fontSize: 12, color: Color(0xff333333))),
            ))
      ],
    );
  }

  Widget _buildToolFunctionItem(int index, AgentToolFunction function) {
    return MouseRegion(
      onEnter: (event) => logic.toolHoverItemId.value = index.toString(),
      onExit: (event) => logic.toolHoverItemId.value = "",
      child: Obx(() {
        var isSelect = logic.toolHoverItemId.value == index.toString();
        var backgroundColor = isSelect ? const Color(0xfff5f5f5) : Colors.transparent;
        return Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: backgroundColor, borderRadius: BorderRadius.circular(8)),
            //color: backgroundColor,
            child: Row(children: [
              Container(
                  width: 40,
                  height: 40,
                  padding: const EdgeInsets.all(10),
                  margin: const EdgeInsets.only(right: 10),
                  decoration: BoxDecoration(color: const Color(0xffe8e8e8), borderRadius: BorderRadius.circular(4)),
                  child: buildAssetImage("icon_default_tool.png", 0, Colors.black)),
              Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text("${function.toolName}/${function.requestMethod?.toUpperCase()}${function.functionName.replaceFirst("/", "_")}",
                    style: const TextStyle(fontSize: 14, color: Color(0xff333333)), maxLines: 1, overflow: TextOverflow.ellipsis),
                Text(function.functionDescription,
                    style: const TextStyle(fontSize: 14, color: Color(0xff999999)), maxLines: 1, overflow: TextOverflow.ellipsis)
              ])),
              const SizedBox(width: 10),
              InkWell(onTap: () => logic.removeFunction(index), child: buildAssetImage("icon_delete.png", 20, Colors.black)),
            ]));
      }),
    );
  }

  Widget _buildLibraryItem(int index, LibraryDto library) {
    return MouseRegion(
      onEnter: (event) => logic.libraryHoverItemId.value = index.toString(),
      onExit: (event) => logic.libraryHoverItemId.value = "",
      child: Obx(() {
        var isSelect = logic.libraryHoverItemId.value == index.toString();
        var backgroundColor = isSelect ? const Color(0xfff5f5f5) : Colors.transparent;
        return Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: backgroundColor, borderRadius: BorderRadius.circular(8)),
            //color: backgroundColor,
            child: Row(children: [
              Container(
                  width: 30,
                  height: 30,
                  padding: const EdgeInsets.all(8),
                  margin: const EdgeInsets.only(right: 10),
                  decoration: BoxDecoration(color: const Color(0xffe8e8e8), borderRadius: BorderRadius.circular(4)),
                  child: buildAssetImage("icon_document.png", 0, Colors.black)),
              Expanded(
                  child: Text(library.name,
                      style: const TextStyle(fontSize: 14, color: Colors.black), maxLines: 1, overflow: TextOverflow.ellipsis)),
              InkWell(onTap: () => logic.removeLibrary(index), child: buildAssetImage("icon_delete.png", 20, Colors.black)),
            ]));
      }),
    );
  }

  Container buildTitleContainer() {
    return Container(
        height: 60,
        child: Row(children: [
          Obx(() => Offstage(offstage: logic.isFullScreen.value || Platform.isWindows, child: const SizedBox(width: 50))),
          Container(
              margin: const EdgeInsets.only(left: 10, right: 5),
              child: InkWell(
                  onTap: () => logic.goBack(),
                  child: Container(padding: const EdgeInsets.all(13), child: buildAssetImage("icon_back.png", 16, Colors.black)))),
          Obx(() {
            var iconPath = logic.agent.value?.iconPath ?? "";
            var name = logic.agent.value?.name ?? "";
            return Row(
              children: [
                SizedBox(width: 32, height: 32, child: buildAgentProfileImage(iconPath)),
                Container(
                    margin: const EdgeInsets.symmetric(horizontal: 10),
                    child: Text(name, style: const TextStyle(fontSize: 14, color: Color(0xff333333))))
              ],
            );
          }),
          InkWell(onTap: () => logic.showEditAgentDialog(), child: buildAssetImage("icon_edit.png", 16, Colors.black)),
          const Spacer(),
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 16)),
                  backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                    RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                  )),
              onPressed: () => logic.backToChat(),
              child: const Text('进入聊天', style: TextStyle(color: Colors.white, fontSize: 14))),
          Container(
            margin: const EdgeInsets.symmetric(horizontal: 10),
            child: buildCommonTextButton("保存", 32, 16, () => logic.updateAgentInfo()),
          ),
          DropdownButtonHideUnderline(
              child: DropdownButton2(
                  customButton: buildCommonTextButton("更多", 32, 16, null),
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
          const SizedBox(width: 20)
        ]));
  }

  Widget _buildMessageItem(int index, ChatMessage chatMessage) {
    if (chatMessage.sendRole == ChatRole.User) {
      var iconPath = logic.account?.avatar ?? "";
      return MouseRegion(
          onEnter: (event) => logic.messageHoverItemId.value = index.toString(),
          onExit: (event) => logic.messageHoverItemId.value = "",
          child: Container(
            margin: const EdgeInsets.only(top: 10),
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Flexible(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(8))),
                        child: MarkdownBody(
                            data: chatMessage.message,
                            onTapLink: (text, url, title) async {
                              if (url != null) {
                                WebUtil.openUrl(url);
                              }
                            }),
                      ),
                      Obx(() => Visibility(
                          visible: logic.messageHoverItemId.value == index.toString(),
                          maintainSize: true,
                          maintainAnimation: true,
                          maintainState: true,
                          child: InkWell(
                              onTap: () => logic.copyToClipboard(chatMessage.message),
                              child: Container(
                                  margin: const EdgeInsets.symmetric(vertical: 10),
                                  child: buildAssetImage("icon_copy.png", 16, const Color(0xff999999))))))
                    ],
                  ),
                ),
                Container(width: 25, height: 25, margin: const EdgeInsets.only(left: 10, top: 4), child: buildUserProfileImage(iconPath))
              ],
            ),
          ));
    } else if (chatMessage.sendRole == ChatRole.Agent) {
      var iconPath = logic.agent.value?.iconPath ?? "";
      String message = chatMessage.isLoading ? "正在生成..." : chatMessage.message;
      return MouseRegion(
          onEnter: (event) => logic.messageHoverItemId.value = index.toString(),
          onExit: (event) => logic.messageHoverItemId.value = "",
          child: Container(
            margin: const EdgeInsets.only(top: 10),
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(width: 25, height: 25, margin: const EdgeInsets.only(right: 10, top: 4), child: buildAgentProfileImage(iconPath)),
                Flexible(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if ((chatMessage.thoughtList?.length ?? 0) > 0) buildThoughtProcessColumn(chatMessage),
                      Container(
                        padding: const EdgeInsets.only(top: 10),
                        //decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(8))),
                        child: MarkdownBody(
                            data: message,
                            onTapLink: (text, url, title) async {
                              if (url != null) {
                                WebUtil.openUrl(url);
                              }
                            }),
                      ),
                      if ((chatMessage.childAgentMessageList?.length ?? 0) > 0)
                        ...List.generate(
                          chatMessage.childAgentMessageList?.length ?? 0,
                          (index) => Container(
                              margin: const EdgeInsets.only(top: 5),
                              child: Text(chatMessage.childAgentMessageList![index],
                                  style: const TextStyle(fontSize: 12, color: Color(0xff999999)))),
                        ),
                      Obx(() => Visibility(
                          visible: logic.messageHoverItemId.value == index.toString(),
                          maintainSize: true,
                          maintainAnimation: true,
                          maintainState: true,
                          child: InkWell(
                              onTap: () => logic.copyToClipboard(chatMessage.message),
                              child: Container(
                                  margin: const EdgeInsets.symmetric(vertical: 10),
                                  child: buildAssetImage("icon_copy.png", 16, const Color(0xff999999))))))
                    ],
                  ),
                ),
              ],
            ),
          ));
    } else {
      return Container();
    }
  }

  Column buildThoughtProcessColumn(ChatMessage chatMessage) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
            padding: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                InkWell(
                    onTap: () {
                      chatMessage.isThoughtExpanded = !chatMessage.isThoughtExpanded;
                      logic.chatMessageList.refresh();
                    },
                    child: Row(children: [
                      const Text("思考过程", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
                      Container(
                          margin: const EdgeInsets.only(left: 5),
                          child:
                              buildAssetImage(chatMessage.isThoughtExpanded ? "icon_up.png" : "icon_down.png", 12, const Color(0xff333333)))
                    ])),
                const Spacer()
              ],
            )),
        Offstage(
          offstage: !chatMessage.isThoughtExpanded,
          child: Column(
            children: [
              ...List.generate(
                chatMessage.thoughtList?.length ?? 0,
                (index) => Container(margin: const EdgeInsets.only(top: 5), child: buildThoughtItem(chatMessage.thoughtList![index])),
              )
            ],
          ),
        )
      ],
    );
  }

  Widget buildThoughtItem(Thought thought) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("工具调用", style: TextStyle(fontSize: 14, color: Color(0xff999999))),
        const Text("接收信息:", style: TextStyle(fontSize: 12, color: Color(0xff999999))),
        Container(
            margin: const EdgeInsets.only(left: 20),
            child: Text(thought.sentMessage, style: const TextStyle(fontSize: 12, color: Color(0xff999999)))),
        Text("${thought.roleName}:", style: const TextStyle(fontSize: 12, color: Color(0xff999999))),
        Container(
            margin: const EdgeInsets.only(left: 20),
            child: Text(thought.receivedMessage, style: const TextStyle(fontSize: 12, color: Color(0xff999999)))),
      ],
    );
  }
}
