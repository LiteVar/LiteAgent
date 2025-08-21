import 'dart:io';

import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../widgets/input_box_container.dart';
import '../../widgets/listview_chat_message.dart';
import 'logic.dart';

class AdjustmentPage extends StatelessWidget {
  AdjustmentPage({super.key});

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
    return Row(
      children: [
        Expanded(
          child: Column(
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
                  child: Obx(() => ChatMessageListView(
                        controller: logic.listViewController,
                        chatMessageList: logic.conversation.value.chatMessageList,
                      )),
                ),
              ),
              InputBoxContainer(controller: logic.inputBoxController),
            ],
          ),
        ),
        Obx(() => Offstage(
              offstage: !logic.showThoughtProcessDetail.value,
              child: buildThoughtDetailRow(),
            ))
      ],
    );
  }

  Row buildThoughtDetailRow() {
    return Row(
      children: [
        verticalLine(),
        SizedBox(
          width: 300,
          child: Column(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                child: Row(
                  children: [
                    const Text("过程详情", style: TextStyle(fontSize: 16, color: Color(0xff333333))),
                    const Spacer(),
                    InkWell(
                      onTap: () => logic.showThoughtProcessDetail.value = false,
                      child: buildAssetImage("icon_close.png", 16, null),
                    )
                  ],
                ),
              ),
              horizontalLine(),
              Expanded(
                child: Obx(() => ListView.builder(
                    itemCount: logic.currentSubMessageList.length,
                    itemBuilder: (context, index) => Container(
                          margin: const EdgeInsets.all(10),
                          padding: const EdgeInsets.symmetric(horizontal: 6),
                          child: buildSubMessageItem(logic.currentSubMessageList[index], () => logic.currentSubMessageList.refresh()),
                        ))),
              ),
            ],
          ),
        )
      ],
    );
  }

  Widget buildLeftSettingContainer() {
    return Align(
      alignment: Alignment.topLeft,
      child: SingleChildScrollView(
        child: Container(
          width: 365,
          padding: const EdgeInsets.all(20),
          child: Obx(
            () {
              bool isAutoAgent = logic.agent.value?.autoAgentFlag ?? false;
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (isAutoAgent) buildTypeColumn(),
                  buildModelSelectColumn(isAutoAgent),
                  if (!isAutoAgent) buildInputPromptColumn(),
                  if (isAutoAgent) buildModelColumn(),
                  buildToolOptionColumn(isAutoAgent),
                  if (!isAutoAgent) buildLibraryOptionColumn(),
                  buildAudioColumn(isAutoAgent),
                  if (!isAutoAgent) buildExecutionModeColumn(),
                  if (!isAutoAgent) buildChildAgentColumn(),
                ],
              );
            },
          ),
        ),
      ),
    );
  }

  Column buildModelColumn() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
            margin: const EdgeInsets.only(top: 10),
            child: InkWell(
              onTap: () => logic.isModelExpanded.value = !logic.isModelExpanded.value,
              child: Row(children: [
                Obx(() => buildAssetImage(
                    logic.isModelExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 18, const Color(0xff333333))),
                const Text("模型", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
              ]),
            )),
        const SizedBox(height: 10),
        Obx(() => Offstage(
            offstage: !logic.isModelExpanded.value,
            child: Column(
              children: [
                Container(
                    margin: const EdgeInsets.only(left: 18),
                    child: const Text("在模型库中对模型开启“支持 Auto Multi Agent”后，模型将在此处显示。agent将根据任务自动选择模型。",
                        style: TextStyle(color: Color(0xff999999), fontSize: 12))),
                Obx(() => Container(
                    margin: const EdgeInsets.only(top: 10, left: 10),
                    child: Column(children: [
                      if (logic.autoAgentModelList.isNotEmpty)
                        ...List.generate(
                          !logic.showMoreModel.value && logic.autoAgentModelList.length > 2 ? 2 : logic.autoAgentModelList.length,
                          (index) => _buildModelItem(index, logic.autoAgentModelList[index]),
                        )
                    ]))),
                Obx(() => Offstage(
                      offstage: logic.autoAgentModelList.length <= 2,
                      child: InkWell(
                        onTap: () => logic.showMoreModel.value = !logic.showMoreModel.value,
                        child: Container(
                            margin: const EdgeInsets.symmetric(vertical: 10),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(logic.showMoreModel.value ? "收起" : "更多",
                                    style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                                const SizedBox(width: 10),
                                buildAssetImage("icon_down.png", 12, const Color(0xff2A82E4))
                              ],
                            )),
                      ),
                    )),
                Offstage(
                  offstage: logic.autoAgentModelList.isNotEmpty,
                  child: Container(
                    margin: const EdgeInsets.only(left: 18, bottom: 10),
                    child: Row(
                      children: [
                        const Text("还没添加可用模型，", style: TextStyle(color: Color(0xff666666), fontSize: 12)),
                        InkWell(
                          onTap: () => logic.backToModelPage(),
                          child: const Text("前往模型管理", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                        )
                      ],
                    ),
                  ),
                )
              ],
            ))),
        horizontalLine(),
      ],
    );
  }

  Column buildChildAgentColumn() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
            margin: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                InkWell(
                  onTap: () => logic.isChildAgentsExpanded.value = !logic.isChildAgentsExpanded.value,
                  child: Row(children: [
                    Obx(() => buildAssetImage(logic.isChildAgentsExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 18,
                        const Color(0xff333333))),
                    const Text("子Agent设置", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
                  ]),
                ),
                const Spacer(),
                buildClickButton("icon_add.png", "添加", () => logic.showChildAgentSelectDialog()),
              ],
            )),
        const SizedBox(height: 10),
        Obx(() => Offstage(
            offstage: !logic.isChildAgentsExpanded.value,
            child: Container(
                margin: const EdgeInsets.only(left: 10),
                child: Column(children: [
                  if (logic.childAgentList.isNotEmpty)
                    ...List.generate(
                      logic.childAgentList.length,
                      (index) => _buildChildAgentItem(index, logic.childAgentList[index]),
                    )
                ])))),
        //horizontalLine(),
      ],
    );
  }

  Column buildAudioColumn(bool isAutoAgent) {
    return Column(
      children: [
        const SizedBox(height: 10),
        InkWell(
          onTap: () => logic.isAudioExpanded.value = !logic.isAudioExpanded.value,
          child: Row(children: [
            Obx(() => buildAssetImage(
                logic.isAudioExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 18, const Color(0xff333333))),
            const Text("语音与文本转化配置", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
          ]),
        ),
        Obx(() => Offstage(
            offstage: !logic.isAudioExpanded.value,
            child: Container(
              margin: const EdgeInsets.only(left: 18),
              child: Column(
                children: [
                  Container(
                      margin: const EdgeInsets.only(top: 10, bottom: 10),
                      child: const Text("开启文字转语音后，AI回复的信息中将显示语音播放功能\n开启语音转文字后，可以使用麦克风进行语音输入",
                          style: TextStyle(color: Color(0xff999999), fontSize: 12))),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                          margin: const EdgeInsets.only(top: 6),
                          child: const Text("文字转语音(TTS)", style: TextStyle(fontSize: 12, color: Color(0xff333333)))),
                      const Spacer(),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Obx(() => Checkbox(
                                  value: logic.enableTextToSpeech.value,
                                  activeColor: Colors.blue,
                                  checkColor: Colors.white,
                                  onChanged: (isCheck) {
                                    logic.enableTextToSpeech.value = isCheck ?? false;
                                    if (!logic.enableTextToSpeech.value) {
                                      logic.currentTTSModelId.value = "";
                                      logic.listViewController.setAudioButtonVisible(false);
                                    }
                                    logic.isAgentChangeWithoutSave = true;
                                  })),
                              const Text("开启", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
                            ],
                          ),
                          DropdownButtonHideUnderline(
                              child: DropdownButton2(
                                  customButton: Container(
                                      width: 200,
                                      height: 32,
                                      margin: const EdgeInsets.only(top: 5),
                                      padding: const EdgeInsets.symmetric(horizontal: 12),
                                      decoration:
                                          const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(4))),
                                      child: Obx(() {
                                        final selectedModel =
                                            logic.ttsModelList.firstWhereOrNull((model) => model.id == logic.currentTTSModelId.value);
                                        String name;
                                        if (selectedModel == null) {
                                          name = "请选择TTS模型";
                                        } else if (selectedModel.nickName == null) {
                                          name = "模型${selectedModel.id.lastSixChars}";
                                        } else {
                                          name = selectedModel.nickName!;
                                        }
                                        return Row(
                                          children: [
                                            Text(
                                              name,
                                              style: TextStyle(
                                                  fontSize: 12,
                                                  color: logic.enableTextToSpeech.value ? const Color(0xff333333) : Colors.grey),
                                            ),
                                            const Spacer(),
                                            buildAssetImage("icon_down.png", 12, const Color(0xff333333))
                                          ],
                                        );
                                      })),
                                  dropdownStyleData: const DropdownStyleData(
                                      width: 200,
                                      offset: Offset(-0, -8),
                                      padding: EdgeInsets.symmetric(vertical: 0),
                                      decoration: BoxDecoration(color: Colors.white)),
                                  menuItemStyleData: const MenuItemStyleData(height: 40),
                                  items: [
                                    ...logic.ttsModelList.map((model) => DropdownMenuItem<String>(
                                        value: model.id,
                                        child: Text(
                                          model.nickName ?? "模型${model.id.lastSixChars}",
                                          style: const TextStyle(fontSize: 13, color: Color(0xff333333)),
                                        )))
                                  ],
                                  onChanged: logic.enableTextToSpeech.value
                                      ? (value) {
                                          if (value != null && value is String) {
                                            logic.currentTTSModelId.value = value;
                                            logic.listViewController.setAudioButtonVisible(true);
                                            logic.isAgentChangeWithoutSave = true;
                                          }
                                        }
                                      : null))
                        ],
                      )
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                          margin: const EdgeInsets.only(top: 6),
                          child: const Text("语音转文字(ASR)", style: TextStyle(fontSize: 12, color: Color(0xff333333)))),
                      const Spacer(),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Obx(() => Checkbox(
                                  value: logic.enableSpeechToText.value,
                                  activeColor: Colors.blue,
                                  checkColor: Colors.white,
                                  onChanged: (isCheck) {
                                    logic.enableSpeechToText.value = isCheck ?? false;
                                    if (!logic.enableSpeechToText.value) {
                                      logic.currentASRModelId.value = "";
                                      logic.inputBoxController.setEnableAudioInput(false);
                                    }
                                    logic.isAgentChangeWithoutSave = true;
                                  })),
                              const Text("开启", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
                            ],
                          ),
                          DropdownButtonHideUnderline(
                              child: DropdownButton2(
                                  customButton: Container(
                                      width: 200,
                                      height: 32,
                                      margin: const EdgeInsets.only(top: 5),
                                      padding: const EdgeInsets.symmetric(horizontal: 12),
                                      decoration:
                                          const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(4))),
                                      child: Obx(() {
                                        final selectedModel =
                                            logic.asrModelList.firstWhereOrNull((model) => model.id == logic.currentASRModelId.value);
                                        String name;
                                        if (selectedModel == null) {
                                          name = "请选择ASR模型";
                                        } else if (selectedModel.nickName == null) {
                                          name = "模型${selectedModel.id.lastSixChars}";
                                        } else {
                                          name = selectedModel.nickName!;
                                        }
                                        return Row(
                                          children: [
                                            Text(
                                              name,
                                              style: TextStyle(
                                                  fontSize: 12,
                                                  color: logic.enableSpeechToText.value ? const Color(0xff333333) : Colors.grey),
                                            ),
                                            const Spacer(),
                                            buildAssetImage("icon_down.png", 12, const Color(0xff333333))
                                          ],
                                        );
                                      })),
                                  dropdownStyleData: const DropdownStyleData(
                                      width: 200,
                                      offset: Offset(-0, -8),
                                      padding: EdgeInsets.symmetric(vertical: 0),
                                      decoration: BoxDecoration(color: Colors.white)),
                                  menuItemStyleData: const MenuItemStyleData(height: 40),
                                  items: [
                                    ...logic.asrModelList.map((model) => DropdownMenuItem<String>(
                                          value: model.id,
                                          child: Text(model.nickName ?? "模型${model.id.lastSixChars}",
                                              style: const TextStyle(fontSize: 13, color: Color(0xff333333))),
                                        ))
                                  ],
                                  onChanged: logic.enableSpeechToText.value
                                      ? (value) {
                                          if (value != null && value is String) {
                                            logic.currentASRModelId.value = value;
                                            logic.inputBoxController.setEnableAudioInput(true);
                                            logic.isAgentChangeWithoutSave = true;
                                          }
                                        }
                                      : null))
                        ],
                      )
                    ],
                  )
                ],
              ),
            ))),
        const SizedBox(height: 10),
        if (!isAutoAgent) horizontalLine(),
      ],
    );
  }

  Column buildExecutionModeColumn() {
    return Column(
      children: [
        const SizedBox(height: 10),
        InkWell(
          onTap: () => logic.isExecutionModeExpanded.value = !logic.isExecutionModeExpanded.value,
          child: Row(children: [
            Obx(() => buildAssetImage(
                logic.isExecutionModeExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 18, const Color(0xff333333))),
            const Text("执行策略", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
          ]),
        ),
        Obx(() => Offstage(
            offstage: !logic.isExecutionModeExpanded.value,
            child: Container(
              margin: const EdgeInsets.only(left: 18),
              child: Column(
                children: [
                  buildAgentOptionColumn(),
                  //buildChildAgentContainer(),
                ],
              ),
            ))),
        const SizedBox(height: 10),
        horizontalLine(),
      ],
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
                      width: 220,
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
                      width: 220,
                      offset: Offset(-0, -8),
                      padding: EdgeInsets.symmetric(vertical: 0),
                      decoration: BoxDecoration(color: Colors.white)),
                  menuItemStyleData: const MenuItemStyleData(height: 40),
                  items: const [
                    DropdownMenuItem<String>(value: "GENERAL", child: Text("普通", style: TextStyle(fontSize: 13))),
                    //DropdownMenuItem<String>(value: "DISTRIBUTE", child: Text("分发", style: TextStyle(fontSize: 13))),
                    DropdownMenuItem<String>(value: "REFLECTION", child: Text("反思", style: TextStyle(fontSize: 13))),
                  ],
                  onChanged: (value) {
                    logic.setAgentType(value);
                    logic.isAgentChangeWithoutSave = true;
                  })),
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
                      width: 220,
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
                      width: 220,
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
                    logic.isAgentChangeWithoutSave = true;
                  })),
        ],
      ),
    ]);
  }

  Container buildChildAgentContainer() {
    return Container(
        margin: const EdgeInsets.only(top: 10, bottom: 0),
        child: Row(children: [
          const Text("子Agent", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
          const Spacer(),
          buildClickButton("icon_setting.png", "设置", () => logic.showChildAgentSelectDialog()),
        ]));
  }

  Column buildToolOptionColumn(bool isAutoAgent) {
    return Column(
      children: [
        Container(
            margin: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                InkWell(
                  onTap: () => logic.isToolExpanded.value = !logic.isToolExpanded.value,
                  child: Row(children: [
                    Obx(() => buildAssetImage(
                        logic.isToolExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 18, const Color(0xff333333))),
                    const Text("工具", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
                  ]),
                ),
                const Spacer(),
                //buildClickButton("icon_set.png", "设置", () => logic.showToolSelectDialog()),
                const SizedBox(width: 10),
                if (!isAutoAgent) buildClickButton("icon_add.png", "添加", () => logic.showToolFunctionSelectDialog()),
                const SizedBox(width: 10),
                if (!isAutoAgent) buildToolSettingWidget()
              ],
            )),
        const SizedBox(height: 10),
        Obx(() => Offstage(
            offstage: !logic.isToolExpanded.value,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                    margin: const EdgeInsets.only(left: 18),
                    child: Text(isAutoAgent ? "在工具库中对工具开启“支持 Auto Multi Agent”后，工具将在此处显示。当指令调用工具时，将自动调用。" : "Agent在特定场景下可以调用工具，可以更好执行指令",
                        style: const TextStyle(color: Color(0xff999999), fontSize: 12))),
                Obx(() => Container(
                    margin: const EdgeInsets.only(top: 10, left: 10),
                    child: Column(children: [
                      if (logic.functionList.isNotEmpty)
                        ...List.generate(
                          !logic.showMoreTool.value && logic.functionList.length > 2 ? 2 : logic.functionList.length,
                          (index) => _buildToolFunctionItem(index, logic.functionList[index], isAutoAgent),
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
                                Text(
                                  logic.showMoreTool.value ? "收起" : "更多",
                                  style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4)),
                                ),
                                const SizedBox(width: 10),
                                buildAssetImage(logic.showMoreTool.value ? "icon_up.png" : "icon_down.png", 12, const Color(0xff2A82E4))
                              ],
                            )),
                      ),
                    )),
                Offstage(
                  offstage: !(isAutoAgent && logic.functionList.isEmpty),
                  child: Container(
                    margin: const EdgeInsets.only(left: 18, bottom: 10),
                    child: Row(
                      children: [
                        const Text("还没添加可用工具，", style: TextStyle(color: Color(0xff666666), fontSize: 12)),
                        InkWell(
                          onTap: () => logic.backToToolPage(),
                          child: const Text("前往工具管理", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                        )
                      ],
                    ),
                  ),
                )
              ],
            ))),
        horizontalLine(),
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
                  // TODO: server支持单个方法的排他性后再支持此选项
                  // DropdownMenuItem<String>(
                  //     value: "reject",
                  //     child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                  //       Visibility(
                  //           visible: logic.toolOperationMode.value == OperationMode.REJECT,
                  //           child: buildAssetImage("icon_checked.png", 12, const Color(0xff2A82E4))),
                  //       const SizedBox(width: 5),
                  //       const Text("拒绝", style: TextStyle(fontSize: 14, color: Color(0xff2A82E4)))
                  //     ])),
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
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
            margin: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                InkWell(
                  onTap: () => logic.isLibraryExpanded.value = !logic.isLibraryExpanded.value,
                  child: Row(children: [
                    Obx(() => buildAssetImage(logic.isLibraryExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 18,
                        const Color(0xff333333))),
                    const Text("知识库", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
                  ]),
                ),
                const Spacer(),
                buildClickButton("icon_add.png", "添加", () => logic.showLibrarySelectDialog()),
              ],
            )),
        const SizedBox(height: 10),
        Obx(() => Offstage(
            offstage: !logic.isLibraryExpanded.value,
            child: Column(
              children: [
                Container(
                    margin: const EdgeInsets.only(left: 18),
                    child: const Text("在问答中，agent可以引用知识库的内容回答问题", style: TextStyle(color: Color(0xff999999), fontSize: 12))),
                Obx(() => Container(
                    margin: const EdgeInsets.only(top: 10, left: 10),
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
                                Text(logic.showMoreLibrary.value ? "收起" : "更多",
                                    style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
                                const SizedBox(width: 10),
                                buildAssetImage("icon_down.png", 12, const Color(0xff2A82E4))
                              ],
                            )),
                      ),
                    ))
              ],
            ))),
        horizontalLine(),
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

  Widget buildInputPromptColumn() {
    return Column(
      children: [
        const SizedBox(height: 10),
        Row(
          children: [
            const Text("系统提示词", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
            const Spacer(),
            buildClickButton("icon_search.png", "提示词预览", () => logic.showPromptPreviewDialog()),
          ],
        ),
        Stack(children: [
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
                child: const Icon(Icons.signal_cellular_4_bar, size: 16, color: Color(0xff999999))),
          )
        ]),
      ],
    );
  }

  Widget buildTypeColumn() {
    String description = logic.agent.value?.description ?? "";
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("类型", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
        Container(
          margin: const EdgeInsets.symmetric(vertical: 10),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text("Auto Multi Agent", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
              const SizedBox(height: 5),
              if (description.isNotEmpty) Text(description, style: const TextStyle(fontSize: 14, color: Color(0xff999999))),
            ],
          ),
        ),
        horizontalLine(),
        const SizedBox(height: 10),
      ],
    );
  }

  Widget buildModelSelectColumn(bool isAutoAgent) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(isAutoAgent ? "系统模型" : "模型", style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
        if (isAutoAgent)
          Container(
              margin: const EdgeInsets.only(top: 4),
              child: const Text("负责规划的大语言模型，统筹所有信息", style: TextStyle(fontSize: 12, color: Color(0xff999999)))),
        Container(
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
                        newButton.nickName = "新建模型";
                        list.add(newButton);
                        var selectId = logic.currentModel?.id ?? "";
                        return DropdownButtonHideUnderline(
                          child: DropdownButton2(
                            isExpanded: true,
                            items: list.map<DropdownMenuItem<String>>((ModelBean item) {
                              var textColor = item.id != newButtonTag ? const Color(0xff333333) : const Color(0xff2A82E4);
                              String nickName = item.nickName ?? "模型${item.id.lastSixChars}";
                              return DropdownMenuItem<String>(
                                  value: item.id, child: Text(nickName, style: TextStyle(fontSize: 14, color: textColor)));
                            }).toList(),
                            value: selectId.isEmpty ? null : selectId,
                            onChanged: (value) {
                              if (value != null && value != newButtonTag) {
                                logic.selectModel(value, false);
                                selectId = value;
                              } else {
                                logic.showCreateModelDialog();
                              }
                            },
                            dropdownStyleData: const DropdownStyleData(
                                offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
                          ),
                        );
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
        ),
      ],
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
      ),
    );
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
                  logic.isAgentChangeWithoutSave = true;
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

  Widget _buildToolFunctionItem(int index, AgentToolFunction function, bool isAutoAgent) {
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
              if (!isAutoAgent) const SizedBox(width: 10),
              if (!isAutoAgent)
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

  Widget _buildModelItem(int index, ModelBean model) {
    return MouseRegion(
      onEnter: (event) => logic.modelHoverItemId.value = index.toString(),
      onExit: (event) => logic.modelHoverItemId.value = "",
      child: Obx(() {
        var isSelect = logic.modelHoverItemId.value == index.toString();
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
                  child: buildAssetImage("icon_default_agent.png", 0, Colors.black)),
              Expanded(
                  child: Text(model.nickName ?? model.name,
                      style: const TextStyle(fontSize: 14, color: Colors.black), maxLines: 1, overflow: TextOverflow.ellipsis)),
              //InkWell(onTap: () => logic.removeLibrary(index), child: buildAssetImage("icon_delete.png", 20, Colors.black)),
            ]));
      }),
    );
  }

  Widget _buildChildAgentItem(int index, AgentBean agent) {
    return MouseRegion(
      onEnter: (event) => logic.agentHoverItemId.value = index.toString(),
      onExit: (event) => logic.agentHoverItemId.value = "",
      child: Obx(() {
        var isSelect = logic.agentHoverItemId.value == index.toString();
        var backgroundColor = isSelect ? const Color(0xfff5f5f5) : Colors.transparent;
        return Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: backgroundColor, borderRadius: BorderRadius.circular(8)),
            //color: backgroundColor,
            child: Row(children: [
              SizedBox(width: 30, height: 30, child: buildAgentProfileImage(agent.iconPath)),
              const SizedBox(width: 10),
              Expanded(
                  child: Text(agent.name,
                      style: const TextStyle(fontSize: 14, color: Colors.black), maxLines: 1, overflow: TextOverflow.ellipsis)),
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 10),
                child: Text(agent.agentType == AgentType.REFLECTION ? "反思" : "普通",
                    style: const TextStyle(fontSize: 14, color: Color(0xff999999))),
              ),
              InkWell(onTap: () => logic.removeChildAgent(index), child: buildAssetImage("icon_delete.png", 20, Colors.black)),
            ]));
      }),
    );
  }

  Widget buildTitleContainer() {
    return SizedBox(
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
          Obx(() => Offstage(
              offstage: logic.agent.value?.autoAgentFlag == true,
              child: DropdownButtonHideUnderline(
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
                      })))),
          const SizedBox(width: 20)
        ]));
  }
}
