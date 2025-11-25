import 'dart:async';
import 'dart:io';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/account.dart';
import 'package:lite_agent_client/utils/snowflake_util.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/repositories/agent_repository.dart';
import 'package:lite_agent_client/repositories/tool_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_login.dart';
import 'package:window_manager/window_manager.dart';

import '../../config/routes.dart';
import '../../repositories/model_repository.dart';
import '../../server/local_server/server.dart';
import '../../widgets/dialog/dialog_user_setting.dart';
import '../../widgets/dialog/dialog_common_confirm.dart';
import '../library/logic.dart';

class HomePageLogic extends GetxController with WindowListener {
  static const String PAGE_CHAT = "chat";
  static const String PAGE_AGENT = "agent";
  static const String PAGE_AGENT_IMPORT = "agent_import";
  static const String PAGE_TOOL = "tool";
  static const String PAGE_MODEL = "model";
  static const String PAGE_LIBRARY = "library";

  var currentPage = PAGE_CHAT.obs;
  Rx<AccountDTO?> account = Rx<AccountDTO?>(null);
  var accountAvatar = "";

  late StreamSubscription _subscription;
  late StreamSubscription _agentSubscription;

  @override
  void onInit() async {
    initWindow();
    initEventBus();
    snowFlakeUtil.init();
    await startServer();
    super.onInit();
    if (await accountRepository.isLogin()) {
      syncUserInfo();
    }
  }

  void initWindow() async {
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  @override
  void onClose() {
    _subscription.cancel();
    _agentSubscription.cancel();
    windowManager.removeListener(this);
    super.onClose();
  }

  @override
  void onWindowClose() {
    if (Get.currentRoute == Routes.home) {
      exit(0);
    }
  }

  void maximize() {
    windowManager.maximize();
  }

  void minimize() {
    windowManager.minimize();
  }

  void close() {
    windowManager.close();
  }

  void switchPage(String page) {
    if (currentPage.value == PAGE_AGENT_IMPORT) {
      _showSwitchPageConfirmDialog(page);
      return;
    }

    _doSwitchPage(page);
  }

  void _doSwitchPage(String page) {
    if (page == PAGE_LIBRARY && Get.isRegistered<LibraryLogic>()) {
      LibraryLogic libraryLogic = Get.find();
      libraryLogic.initData();
    }
    currentPage.value = page;
    //update();
  }

  void _showSwitchPageConfirmDialog(String targetPage) {
    Get.dialog(
      barrierDismissible: false,
      CommonConfirmDialog(
          title: '确认切换',
          content: '当前正在导入智能体，切换页面将丢失当前进度，是否确认切换？',
          confirmString: '确认',
          onConfirmCallback: () async => _doSwitchPage(targetPage)),
    );
  }

  void switchToAgentImportPage() {
    currentPage.value = PAGE_AGENT_IMPORT;
  }

  void backToAgentPage() {
    currentPage.value = PAGE_AGENT;
  }

  void showLoginDialog(bool afterLogout) async {
    if (!await accountRepository.isLogin()) {
      Get.dialog(barrierDismissible: false, LoginDialog(noAutoLogin: afterLogout));
    }
  }

  void showSettingDialog() async {
    if (!await accountRepository.isLogin()) {
      Get.dialog(barrierDismissible: false, LoginDialog(noAutoLogin: true));
    } else {
      Get.dialog(barrierDismissible: false, UserSettingDialog());
    }
  }

  void initEventBus() {
    _subscription = eventBus.on<MessageEvent>().listen((event) {
      if (event.message == EventBusMessage.login) {
        syncUserInfo();
      } else if (event.message == EventBusMessage.logout) {
        account.value = null;
        showLoginDialog(true);
      } else if (event.message == EventBusMessage.switchPage) {
        if (event.data != null) {
          switchPage(event.data as String);
        }
      }
    });
    _agentSubscription = eventBus.on<AgentMessageEvent>().listen((event) {
      if (event.message == EventBusMessage.startChat) {
        switchPage(PAGE_CHAT);
      }
    });
  }

  void syncUserInfo() async {
    account.value = await accountRepository.updateUserInfoFromNet();
    if (account.value != null) {
      accountAvatar = await account.value!.avatar.fillPicLinkPrefixAsync();
    }
    if (await accountRepository.isLogin()) {
      await toolRepository.uploadAllToServer();
      await modelRepository.uploadAllToServer();
      await agentRepository.uploadAllToServer();
    }
  }
}
