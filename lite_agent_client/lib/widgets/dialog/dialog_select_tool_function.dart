import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/uitl/snowflake_uitl.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_core_dart/lite_agent_service.dart';

import '../../models/local_data_model.dart';
import '../common_widget.dart';
import 'dialog_tool_edit.dart';

class SelectToolFunctionDialog extends StatelessWidget {
  SelectToolFunctionDialog({super.key, required this.selectToolFunctionList, this.onSelectChanged});

  //ui
  var borderRadius = 16.0;
  var expandList = <String>[];

  //data
  var selectToolFunctionList = <AgentToolFunction>[];
  Function()? onSelectChanged;
  var toolList = <ToolBean>[].obs;

  var mcpService;

  Future<void> initData() async {
    toolList.assignAll((await toolRepository.getToolListFromBox()));
    //make sure functionList will init
    for (var tool in toolList) {
      tool.functionList.clear();
      if (tool.schemaType != Protocol.MCP_STDIO_TOOLS) {
        tool.initToolFunctionList();
      }
    }
  }

  bool isFunctionSelected(AgentToolFunction function) {
    for (var item in selectToolFunctionList) {
      if (item.toolId == function.toolId && item.functionName == function.functionName && item.requestMethod == function.requestMethod) {
        return true;
      }
    }
    return false;
  }

  void toggleFunctionSelectStatus(AgentToolFunction function) {
    bool isSelected = false;
    for (var item in selectToolFunctionList) {
      if (item.toolId == function.toolId && item.functionName == function.functionName && item.requestMethod == function.requestMethod) {
        isSelected = true;
        selectToolFunctionList.remove(item);
        onSelectChanged?.call();
        break;
      }
    }
    if (!isSelected) {
      if (function.isThirdTool ?? false) {
        for (var item in selectToolFunctionList) {
          if ((item.isThirdTool ?? false) && item.toolId != function.toolId) {
            AlarmUtil.showAlertToast("暂不支持选择多个工具的第三方tool回调");
            return;
          }
        }
      }
      selectToolFunctionList.add(function);
      onSelectChanged?.call();
    }
    toolList.refresh();
  }

  Future<void> toggleItemExpandStatus(String id) async {
    if (expandList.contains(id)) {
      expandList.remove(id);
    } else {
      expandList.add(id);
      for (var tool in toolList) {
        if (tool.id == id && tool.functionList.isEmpty) {
          //emptyList means not init yet
          await tool.initToolFunctionList(showLoading: true);
          toolList.refresh();
          break;
        }
      }
    }
    toolList.refresh();
  }

  bool isMCPSeverSelected(ToolBean tool) {
    if (tool.schemaType != Protocol.MCP_STDIO_TOOLS) {
      return false;
    }
    for (var item in selectToolFunctionList) {
      if (item.toolId == tool.id) {
        return true;
      }
    }
    return false;
  }

  Future<void> toggleAllFunctionSelectStatus(ToolBean tool) async {
    if (tool.schemaType == Protocol.MCP_STDIO_TOOLS) {
      bool isSelected = isMCPSeverSelected(tool);
      for (var item in selectToolFunctionList) {
        if (item.toolId == tool.id) {
          isSelected = true;
          selectToolFunctionList.remove(item);
          break;
        }
      }
      if (!isSelected) {
        AgentToolFunction function = AgentToolFunction()
          ..toolId = tool.id
          ..toolName = tool.name;
        selectToolFunctionList.add(function);
      }
    } else {
      if (await isAllFunctionSelected(tool)) {
        for (var function in tool.functionList) {
          if (isFunctionSelected(function)) {
            toggleFunctionSelectStatus(function);
          }
        }
      } else {
        for (var function in tool.functionList) {
          if (!isFunctionSelected(function)) {
            toggleFunctionSelectStatus(function);
          }
        }
      }
    }
    toolList.refresh();
  }

  bool isAllFunctionSelected(ToolBean tool) {
    for (var function in tool.functionList) {
      if (!isFunctionSelected(function)) {
        return false;
      }
    }
    return true;
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
              buildTabContainer(),
              Expanded(
                  child: Obx(() => ListView.builder(
                        itemCount: toolList.length,
                        itemBuilder: (context, index) => buildListItem(toolList[index]),
                      ))),
            ])));
  }

  Widget buildTabContainer() {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 15, horizontal: 20),
      child: Row(children: [
        const Spacer(),
        buildCommonTextBlueButton("新建工具", () => _showCreateToolDialog()),
      ]),
    );
  }

  Container buildTitleContainer() {
    return Container(
      margin: const EdgeInsets.only(bottom: 5),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(borderRadius), topRight: Radius.circular(borderRadius)),
      ),
      child: Row(children: [
        const Text("添加工具"),
        const Spacer(),
        InkWell(
            onTap: () => Get.back(),
            child: Container(margin: const EdgeInsets.only(right: 2), child: buildAssetImage("icon_close.png", 16, Colors.black)))
      ]),
    );
  }

  Container buildListItem(ToolBean tool) {
    var isMcpServer = tool.schemaType == Protocol.MCP_STDIO_TOOLS;
    var isSelected = isMcpServer ? isMCPSeverSelected(tool) : isAllFunctionSelected(tool);
    var mcpButtonText = isSelected ? "移除" : "添加";
    var normalButtonText = isSelected ? "移除所有方法" : "添加所有方法";
    var buttonTextColor = isSelected ? const Color(0xffF24E4E) : const Color(0xff2A82E4);
    var buttonColor = isSelected ? const Color(0xffFFE8E8) : const Color(0xffE8F2FF);
    bool isExpand = expandList.contains(tool.id);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                  width: 36,
                  height: 36,
                  padding: const EdgeInsets.all(6),
                  decoration: const BoxDecoration(color: Color(0xffe8e8e8), borderRadius: BorderRadius.all(Radius.circular(4))),
                  child: buildAssetImage("icon_default_tool.png", 0, Colors.black)),
              Expanded(
                child: Container(
                  margin: const EdgeInsets.symmetric(horizontal: 15),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(tool.name, style: const TextStyle(fontSize: 16, color: Colors.black)),
                      Offstage(
                        offstage: tool.description.isEmpty,
                        child: Text(tool.description,
                            maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xff999999))),
                      )
                    ],
                  ),
                ),
              ),
              Container(
                  margin: const EdgeInsets.symmetric(horizontal: 10),
                  child: TextButton(
                      style: ButtonStyle(
                          padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 16)),
                          backgroundColor: WidgetStateProperty.all(buttonColor),
                          shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                            RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                          )),
                      onPressed: () => toggleAllFunctionSelectStatus(tool),
                      child: Text(isMcpServer ? mcpButtonText : normalButtonText, style: TextStyle(color: buttonTextColor, fontSize: 14)))),
              InkWell(
                  onTap: () => toggleItemExpandStatus(tool.id),
                  child: buildAssetImage(isExpand ? "icon_up.png" : "icon_down.png", 16, const Color(0xff999999)))

              //buildCommonTextBlueButton(buttonText, () => toggleSelectStatus(tool.id, isSelected))
            ],
          ),
          Container(margin: const EdgeInsets.symmetric(vertical: 15), child: horizontalLine()),
          Offstage(
            offstage: !isExpand,
            child: Column(
              children: [
                ...List.generate(
                  tool.functionList.length,
                  (index) => buildFunctionItem(tool.functionList[index], !isMcpServer),
                )
              ],
            ),
          )
        ],
      ),
    );
  }

  Widget buildFunctionItem(AgentToolFunction agentFunction, bool showSelectionButton) {
    var isSelected = isFunctionSelected(agentFunction);
    var buttonText = isSelected ? "移除" : "添加";
    var buttonTextColor = isSelected ? const Color(0xffF24E4E) : const Color(0xff2A82E4);
    var buttonColor = isSelected ? const Color(0xffFFE8E8) : const Color(0xffE8F2FF);
    return Column(
      children: [
        Row(children: [
          Expanded(
              child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text("${agentFunction.requestMethod?.toUpperCase()}${agentFunction.functionName.replaceFirst("/", "_")}",
                  style: const TextStyle(fontSize: 14, color: Color(0xff333333))),
              const SizedBox(height: 5),
              Text(agentFunction.functionDescription,
                  maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14, color: Color(0xff999999))),
            ],
          )),
          const SizedBox(width: 10),
          if (showSelectionButton)
            TextButton(
                style: ButtonStyle(
                    padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 16)),
                    backgroundColor: WidgetStateProperty.all(buttonColor),
                    shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                      RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
                    )),
                onPressed: () => toggleFunctionSelectStatus(agentFunction),
                child: Text(buttonText, style: TextStyle(color: buttonTextColor, fontSize: 14)))
        ]),
        Container(margin: const EdgeInsets.symmetric(vertical: 15), child: horizontalLine()),
      ],
    );
  }

  void _showCreateToolDialog() {
    Get.dialog(barrierDismissible: false, EditToolDialog(tool: null, isEdit: false, onConfirmCallback: _createNewTool));
  }

  void _createNewTool(
      String name, String description, String schemaType, String schemaText, String apiType, String apiText, bool supportMultiAgent) {
    ToolBean targetTool = ToolBean()
      ..id = snowFlakeUtil.getId()
      ..createTime = DateTime.now().microsecondsSinceEpoch
      ..name = name
      ..description = description
      ..schemaType = schemaType
      ..schemaText = schemaText
      ..apiText = apiText
      ..apiType = apiType
      ..supportMultiAgent = supportMultiAgent;

    toolList.add(targetTool);
    toolList.refresh();

    toolRepository.updateTool(targetTool.id, targetTool);
    eventBus.fire(ToolMessageEvent(message: EventBusMessage.updateList));
  }
}
