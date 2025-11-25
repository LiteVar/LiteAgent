import 'dart:io';

import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/chat_message_items.dart';
import 'package:lite_agent_client/widgets/common_button.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'widgets/dialog_manager.dart';
import '../../widgets/input_box_container.dart';
import '../../widgets/listview_chat_message.dart';
import 'logic.dart';
import 'widgets/agent/agent_params_popup.dart';
import 'widgets/agent/auto_agent_type.dart';
import 'widgets/agent/child_agent_list.dart';
import 'widgets/agent/execution_mode.dart';
import 'widgets/agent/input_prompt_config.dart';
import 'widgets/library/library_list.dart';
import 'widgets/model/auto_agent_model_list.dart';
import 'widgets/model/model_selector.dart';
import 'widgets/model/voice_config_widget.dart';
import 'widgets/tool/tool_function_list.dart';

class AdjustmentPage extends StatelessWidget {
  AdjustmentPage({super.key});

  final logic = Get.put(AdjustmentLogic());
  late final DialogManager dialogManager = DialogManager(logic);

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
                    CommonBlueButton(iconName: "icon_clear.png", buttonText: "清除", onTap: () => logic.clearAllMessage()),
                  ])),
              Expanded(
                child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 20),
                  child: Obx(() => ChatMessageListView(
                        controller: logic.chatService.listViewController,
                        chatMessageList: logic.chatService.conversation.value.chatMessageList,
                      )),
                ),
              ),
              InputBoxContainer(controller: logic.chatService.inputBoxController),
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
                          child: ChatMessageItem.buildSubMessageItem(
                              logic.currentSubMessageList[index], () => logic.currentSubMessageList.refresh()),
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
                  if (isAutoAgent) const AutoAgentType(),
                  _buildModelSelectionRow(isAutoAgent),
                  if (isAutoAgent)
                    AutoAgentModelList(
                      modelStateManager: logic.modelStateManager,
                      onBackToModelPage: () => logic.backToModelPage(),
                    ),
                  if (!isAutoAgent)
                    InputPromptConfig(
                      textController: logic.promptController,
                      onDataChanged: () => logic.isAgentChangeWithoutSave = true,
                    ),
                  ToolFunctionList(
                    isAutoAgent: isAutoAgent,
                    toolStateManager: logic.toolStateManager,
                    currentAgentId: logic.agent.value?.id ?? "",
                    onBackToToolPage: () => logic.backToToolPage(),
                    onDataChanged: () => logic.isAgentChangeWithoutSave = true,
                  ),
                  if (!isAutoAgent)
                    LibraryList(
                      libraryStateManager: logic.libraryStateManager,
                      isLogin: logic.isLogin,
                      onDataChanged: () => logic.isAgentChangeWithoutSave = true,
                    ),
                  VoiceConfigWidget(
                    modelStateManager: logic.modelStateManager,
                    isAutoAgent: isAutoAgent,
                    onDataChanged: () => logic.isAgentChangeWithoutSave = true,
                    onTTSEnabled: (enabled) {
                      logic.chatService.listViewController.setAudioButtonVisible(enabled);
                    },
                    onASREnabled: (enabled) {
                      logic.chatService.inputBoxController.setEnableAudioInput(enabled);
                    },
                  ),
                  if (!isAutoAgent)
                    ExecutionMode(
                      agentStateManager: logic.agentStateManager,
                      onDataChanged: () => logic.isAgentChangeWithoutSave = true,
                    ),
                  if (!isAutoAgent)
                    ChildAgentList(
                      agentStateManager: logic.agentStateManager,
                      onDataChanged: () => logic.isAgentChangeWithoutSave = true,
                      onShowChildAgentSelectDialog: () => dialogManager.showChildAgentSelectDialog(),
                    ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }

  Widget _buildModelSelectionRow(bool isAutoAgent) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(isAutoAgent ? "系统模型" : "模型", style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
        if (isAutoAgent)
          Container(
            margin: const EdgeInsets.only(top: 4),
            child: const Text("负责规划的大语言模型，统筹所有信息", style: TextStyle(fontSize: 12, color: Color(0xff999999))),
          ),
        Container(
          margin: const EdgeInsets.symmetric(vertical: 10),
          child: Row(
            children: [
              Expanded(
                child: ModelSelector(
                  modelStateManager: logic.modelStateManager,
                  onModelSelected: (modelId) => logic.selectModel(modelId, false),
                  onCreateModel: () => dialogManager.showCreateModelDialog(),
                ),
              ),
              const SizedBox(width: 10),
              AgentParamsPopup(
                currentModel: logic.modelStateManager.currentLLMModel,
                sliderTempValue: logic.agentStateManager.sliderTempValue,
                sliderTokenValue: logic.agentStateManager.sliderTokenValue,
                sliderTopPValue: logic.agentStateManager.sliderTopPValue,
                onDataChanged: () => logic.isAgentChangeWithoutSave = true,
              ),
            ],
          ),
        ),
      ],
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
                  onTap: () {
                    if (logic.isAgentChangeWithoutSave) {
                      dialogManager.showGoBackConfirmDialog();
                    } else {
                      Get.back();
                    }
                  },
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
          InkWell(onTap: () => dialogManager.showEditAgentDialog(), child: buildAssetImage("icon_edit.png", 16, Colors.black)),
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
                  child: DropdownButton2<String>(
                      customButton: buildCommonTextButton("更多", 32, 16, null),
                      dropdownStyleData: DropdownStyleData(
                          width: 96,
                          offset: const Offset(-8, -6),
                          padding: const EdgeInsets.symmetric(vertical: 4),
                          decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(8),
                              boxShadow: const [BoxShadow(color: Color(0x1A000000), blurRadius: 8, offset: Offset(0, 2))])),
                      menuItemStyleData: const MenuItemStyleData(height: 40),
                      items: const [
                        DropdownMenuItem<String>(
                          value: "export",
                          child: Center(child: Text("导出", style: TextStyle(fontSize: 14, color: Color(0xFF333333)))),
                        ),
                        DropdownMenuItem<String>(
                          value: "delete",
                          child: Center(child: Text("删除", style: TextStyle(fontSize: 14, color: Color(0xFF333333)))),
                        ),
                      ],
                      onChanged: (value) {
                        if (value == "delete") {
                          var agentId = logic.agent.value?.id ?? "";
                          dialogManager.showRemoveAgentDialog(agentId);
                        } else if (value == "export") {
                          dialogManager.showExportConfirmDialog();
                        }
                      })))),
          const SizedBox(width: 20)
        ]));
  }

}
