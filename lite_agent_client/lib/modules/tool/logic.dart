import 'dart:async';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/utils/snowflake_util.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/utils/extension/tool_extension.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_tool_detail.dart';
import 'package:window_manager/window_manager.dart';

import '../../repositories/account_repository.dart';
import '../../widgets/dialog/dialog_common_confirm.dart';
import '../../widgets/dialog/dialog_login.dart';
import '../../widgets/dialog/tool_edit/dialog.dart';
import '../../widgets/dialog/tool_edit/controller.dart';

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
  var currentToolList = <ToolModel>[].obs;
  final _toolListMap = <String, List<ToolModel>>{};

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
    _toolListMap[TAB_LOCAL] = <ToolModel>[];
    _toolListMap[TAB_SEC_ALL] = <ToolModel>[];
    _toolListMap[TAB_SEC_SYSTEM] = <ToolModel>[];
    _toolListMap[TAB_SEC_SHARE] = <ToolModel>[];
    _toolListMap[TAB_SEC_MINE] = <ToolModel>[];
    await loadLocalData(false);
    await loadCloudData(false);
    switchTab(TAB_LOCAL);
  }

  Future<void> loadLocalData(bool refresh) async {
    _toolListMap[TAB_LOCAL]!.assignAll((await toolRepository.getToolListFromBox()));
    _toolListMap[TAB_LOCAL]!.sort((a, b) => (b.createTime ?? 0) - (a.createTime ?? 0));
    if (refresh) {
      currentToolList.refresh();
    }
  }

  Future<void> loadCloudData(bool refresh) async {
    _toolListMap[TAB_SEC_ALL]!.assignAll((await toolRepository.getCloudAgentListAndTranslate(0)));
    _toolListMap[TAB_SEC_SYSTEM]!.assignAll((await toolRepository.getCloudAgentListAndTranslate(1)));
    _toolListMap[TAB_SEC_SHARE]!.assignAll((await toolRepository.getCloudAgentListAndTranslate(2)));
    _toolListMap[TAB_SEC_MINE]!.assignAll((await toolRepository.getCloudAgentListAndTranslate(3)));
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

  void updateLocalTool(String id, ToolFormData toolData) async {
    ToolModel? targetTool;
    if (id.isNotEmpty) {
      targetTool = await toolRepository.getToolFromBox(id);
    }
    targetTool ??= ToolModel.newEmptyTool(id: snowFlakeUtil.getId(), createTime: DateTime.now().microsecondsSinceEpoch);
    targetTool.name = toolData.name;
    targetTool.description = toolData.description;
    targetTool.schemaType = toolData.schemaType;
    targetTool.schemaText = toolData.schemaText;
    targetTool.apiText = toolData.apiText;
    targetTool.apiType = toolData.apiType;
    targetTool.supportMultiAgent = toolData.supportMultiAgent;
    toolRepository.updateTool(targetTool.id, targetTool);

    List<ToolModel> localList = _toolListMap[TAB_LOCAL]!;
    bool isAdd = false;
    for (var tool in localList) {
      if (tool.id == targetTool.id) {
        int index = localList.indexOf(tool);
        localList[index] = targetTool;
        isAdd = true;
        break;
      }
    }
    if (!isAdd) {
      localList.add(targetTool);
    }
    currentToolList.refresh();
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

  void showEditToolDialog(ToolModel? tool) async {
    Get.dialog(
        barrierDismissible: false,
        EditToolDialog(
          tool: tool,
          isEdit: tool != null,
          onConfirmCallback: (ToolFormData? toolData, {bool isDelete = false}) async {
            if (isDelete && tool != null) {
              var localList = _toolListMap[TAB_LOCAL];
              if (localList != null) {
                localList.remove(tool);
              }
              toolRepository.removeTool(tool.id);
              currentToolList.refresh();
              return;
            }
            if (toolData != null) {
              updateLocalTool(tool?.id ?? "", toolData);
              switchTab(TAB_LOCAL);
            }
          },
        ));
  }

  Future<void> showToolDetailDialog(ToolModel tool) async {
    if (tool.isCloud) {
      var dto = await toolRepository.getCloudToolDetail(tool.id);
      if (dto != null) {
        tool = dto.toModel();
      }
    }
    Get.dialog(ToolDetailDialog(tool: tool));
  }

  void onLoginButtonClick() {
    Get.dialog(barrierDismissible: false, LoginDialog());
  }
}
