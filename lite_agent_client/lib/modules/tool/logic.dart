import 'dart:async';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/uitl/snowflake_uitl.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_tool_detail.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_tool_edit.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/dto/tool.dart';
import '../../models/local_data_model.dart';
import '../../repositories/account_repository.dart';
import '../../widgets/dialog/dialog_common_confirm.dart';
import '../../widgets/dialog/dialog_login.dart';

class ToolLogic extends GetxController with WindowListener {
  static const String TAB_LOCAL = "local";
  static const String TAB_CLOUD = "cloud";
  static const String TAB_SEC_ALL = "secondary_all";
  static const String TAB_SEC_SYSTEM = "secondary_system";
  static const String TAB_SEC_SHARE = "secondary_share";
  static const String TAB_SEC_MINE = "secondary_mine";

  late StreamSubscription _subscription;
  late StreamSubscription _toolSubscription;

  var currentTab = TAB_LOCAL.obs;
  var currentSecondaryTab = TAB_SEC_ALL.obs;
  var currentToolList = <ToolDTO>[].obs;
  final Map<String, List<ToolDTO>> _toolListMap = <String, List<ToolDTO>>{};

  var isLogin = false;

  @override
  void onInit() {
    super.onInit();
    initWindow();
    initData();
    initEventBus();
  }

  void initWindow() async {
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  Future<void> initData() async {
    isLogin = (await accountRepository.isLogin());
    _toolListMap[TAB_LOCAL] = <ToolDTO>[];
    _toolListMap[TAB_SEC_ALL] = <ToolDTO>[];
    _toolListMap[TAB_SEC_SYSTEM] = <ToolDTO>[];
    _toolListMap[TAB_SEC_SHARE] = <ToolDTO>[];
    _toolListMap[TAB_SEC_MINE] = <ToolDTO>[];
    await loadLocalData(false);
    await loadCloudData(false);
    switchTab(TAB_LOCAL);
  }

  Future<void> loadLocalData(bool refresh) async {
    _toolListMap[TAB_LOCAL]!.assignAll((await toolRepository.getToolDTOListFromBox()));
    if (refresh) {
      currentToolList.refresh();
    }
  }

  Future<void> loadCloudData(bool refresh) async {
    _toolListMap[TAB_SEC_ALL]!.assignAll((await toolRepository.getCloudToolList(0)));
    _toolListMap[TAB_SEC_SYSTEM]!.assignAll((await toolRepository.getCloudToolList(1)));
    _toolListMap[TAB_SEC_SHARE]!.assignAll((await toolRepository.getCloudToolList(2)));
    _toolListMap[TAB_SEC_MINE]!.assignAll((await toolRepository.getCloudToolList(3)));
    if (refresh) {
      currentToolList.refresh();
    }
  }

  void initEventBus() {
    _subscription = eventBus.on<MessageEvent>().listen((event) {
      if (event.message == EventBusMessage.login) {
        isLogin = true;
        currentTab.refresh();
        loadCloudData(true);
      } else if (event.message == EventBusMessage.logout) {
        isLogin = false;
        currentTab.refresh();
      }
    });
    _toolSubscription = eventBus.on<ToolMessageEvent>().listen((event) {
      if (event.message == EventBusMessage.updateList) {
        loadLocalData(true);
      }
    });
  }

  @override
  void onClose() {
    _subscription.cancel();
    _toolSubscription.cancel();
    super.onClose();
  }

  @override
  void onWindowClose() {
    Get.back();
  }

  void removeTool(String id) {
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
          title: "删除确认",
          content: "即将删除工具的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () async {
            var localList = _toolListMap[TAB_LOCAL];
            if (localList == null) {
              return;
            }
            for (var tool in localList) {
              if (tool.id == id) {
                localList.remove(tool);
                toolRepository.removeTool(id);
                currentToolList.refresh();
                break;
              }
            }
          },
        ));
  }

  void updateLocalTool(String id, String name, String description, String schemaType, String schemaText, String thirdSchemaText,
      String apiType, String apiText) async {
    var localList = _toolListMap[TAB_LOCAL];
    if (localList == null) {
      return;
    }
    String targetId = id;
    if (targetId.isNotEmpty) {
      for (var tool in localList) {
        if (tool.id == targetId) {
          tool.name = name;
          tool.description = description;
          break;
        }
      }
    } else {
      targetId = snowFlakeUtil.getId();
      var tool = ToolDTO(targetId, "", "", name, description, 0, "", "", "", "", false, "", ""); //just for showing
      localList.add(tool);
    }
    currentToolList.refresh();

    ToolBean? targetTool = await toolRepository.getToolFromBox(targetId);
    if (targetTool == null) {
      targetTool = ToolBean();
      targetTool.id = targetId;
      targetTool.createTime = DateTime.now().microsecondsSinceEpoch;
    }
    targetTool.name = name;
    targetTool.description = description;
    targetTool.schemaText = schemaText;
    targetTool.thirdSchemaText = thirdSchemaText;
    targetTool.schemaType = schemaType;
    targetTool.apiText = apiText;
    targetTool.apiType = apiType;
    toolRepository.updateTool(targetId, targetTool);
  }

  void switchTab(String tabType) {
    if (tabType == TAB_LOCAL || tabType == TAB_CLOUD) {
      currentTab.value = tabType;
    } else {
      currentSecondaryTab.value = tabType;
    }
    if (currentTab.value == TAB_LOCAL) {
      currentToolList.value = _toolListMap[TAB_LOCAL]!;
    } else {
      currentToolList.value = _toolListMap[currentSecondaryTab.value]!;
    }
  }

  void showCreateToolDialog() {
    showEditToolDialog(null);
  }

  void showEditToolDialog(String? toolId) async {
    ToolBean? tool;
    if (toolId != null) {
      tool = await toolRepository.getToolFromBox(toolId);
    }
    Get.dialog(
        barrierDismissible: false,
        EditToolDialog(
          tool: tool,
          isEdit: tool != null,
          onConfirmCallback: (String name, String description, String schemaType, String schemaText, String thirdSchemaText, String apiType,
              String apiText) {
            updateLocalTool(tool?.id ?? "", name, description, schemaType, schemaText, thirdSchemaText, apiType, apiText);
            switchTab(TAB_LOCAL);
          },
        ));
  }

  Future<void> showToolDetailDialog(String toolId) async {
    var tool = await toolRepository.getCloudToolDetail(toolId);
    if (tool != null) {
      ToolBean toolBean = ToolBean();
      toolBean.name = tool.name ?? "";
      toolBean.description = tool.description ?? "";
      toolBean.schemaText = tool.schemaStr ?? "";
      String schemaType = "";
      switch (tool.schemaType) {
        case 1:
          schemaType = "openapi";
          break;
        case 2:
          schemaType = "jsonrpc";
          break;
        case 3:
          schemaType = "open_modbus";
          break;
      }
      toolBean.schemaType = schemaType;
      toolBean.apiText = tool.apiKey ?? "";
      toolBean.apiType = tool.apiKeyType ?? "";
      Get.dialog(ToolDetailDialog(tool: toolBean));
    }
  }

  void onLoginButtonClick() {
    Get.dialog(barrierDismissible: false, LoginDialog());
  }
}
