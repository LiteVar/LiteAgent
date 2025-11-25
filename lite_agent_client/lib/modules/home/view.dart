import 'dart:io';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/modules/agent/view.dart';
import 'package:lite_agent_client/modules/agent_import/view.dart';
import 'package:lite_agent_client/modules/chat/view.dart';
import 'package:lite_agent_client/modules/library/view.dart';
import 'package:lite_agent_client/modules/model/view.dart';
import 'package:lite_agent_client/modules/tool/view.dart';

import '../../widgets/common_widget.dart';
import 'logic.dart';

class HomePage extends GetResponsiveView<HomePageLogic> {
  HomePage({super.key}) : super(alwaysUseBuilder: false);

  final bgColor = const Color(0xFF001528);

  @override
  Widget? desktop() {
    final logic = Get.put(HomePageLogic());
    return Scaffold(
      body: Row(
        children: [
          if (Platform.isMacOS) _buildMacOSMenu(logic),
          Container(
            width: 72,
            color: bgColor,
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Column(
              children: [
                const SizedBox(height: 48),
                SizedBox(
                    height: 42,
                    child: Center(child: Obx(() {
                      var isLogin = logic.account.value != null;
                      if (!isLogin) {
                        /*return InkWell(
                            onTap: () => logic.showLoginDialog(false),
                            child: const Column(
                              children: [
                                Icon(Icons.person, color: Colors.white, size: 24.0),
                                Text("未登录", style: TextStyle(fontSize: 12, color: Colors.white))
                              ],
                            ));*/
                        return Container();
                      } else {
                        return SizedBox(height: 24, width: 24, child: buildUserProfileImage(logic.accountAvatar));
                      }
                    }))),
                const SizedBox(height: 40),
                Obx(() => Column(
                      children: [
                        _createFuncItem('聊天', "icon_message.png", logic.currentPage.value == HomePageLogic.PAGE_CHAT,
                            () => logic.switchPage(HomePageLogic.PAGE_CHAT)),
                        _createFuncItem('Agents', "icon_robot.png", logic.currentPage.value == HomePageLogic.PAGE_AGENT || logic.currentPage.value == HomePageLogic.PAGE_AGENT_IMPORT,
                            () => logic.switchPage(HomePageLogic.PAGE_AGENT)),
                        _createFuncItem('工具', "icon_printer.png", logic.currentPage.value == HomePageLogic.PAGE_TOOL,
                            () => logic.switchPage(HomePageLogic.PAGE_TOOL)),
                        _createFuncItem('大模型', "icon_table.png", logic.currentPage.value == HomePageLogic.PAGE_MODEL,
                            () => logic.switchPage(HomePageLogic.PAGE_MODEL)),
                        _createFuncItem('知识库', "icon_document.png", logic.currentPage.value == HomePageLogic.PAGE_LIBRARY,
                            () => logic.switchPage(HomePageLogic.PAGE_LIBRARY))
                      ],
                    )),
                const Spacer(),
                _createFuncItem('设置', "icon_setting.png", false, () => logic.showSettingDialog()),
              ],
            ),
          ),
          Expanded(child: Obx(() {
            switch (logic.currentPage.value) {
              case HomePageLogic.PAGE_CHAT:
                return ChatPage();
              case HomePageLogic.PAGE_AGENT:
                return AgentPage();
              case HomePageLogic.PAGE_AGENT_IMPORT:
                return AgentImportPage();
              case HomePageLogic.PAGE_TOOL:
                return ToolPage();
              case HomePageLogic.PAGE_MODEL:
                return ModelPage();
              case HomePageLogic.PAGE_LIBRARY:
                return LibraryPage();
              default:
                return Container();
            }
          }))
        ],
      ),
    );
  }

  Widget _createFuncItem(String title, String iconFileName, bool isSelect, Function()? onTap) {
    var itemColor = isSelect ? Colors.blue : Colors.white;
    return InkWell(
        onTap: onTap,
        child: SizedBox(
          width: 72,
          height: 64,
          child: Center(
              child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              buildAssetImage(iconFileName, 16, itemColor),
              const SizedBox(height: 4),
              Text(title, style: TextStyle(color: itemColor, fontSize: 12))
            ],
          )),
        ));
  }

  Widget _buildMacOSMenu(HomePageLogic logic) {
    return PlatformMenuBar(menus: <PlatformMenuItem>[
      const PlatformMenu(
        label: 'LiteAgent',
        menus: <PlatformMenuItem>[
          PlatformProvidedMenuItem(type: PlatformProvidedMenuItemType.quit),
        ],
      ),
      PlatformMenu(label: '文件', menus: <PlatformMenuItem>[
        PlatformMenuItem(label: 'agent商店', onSelected: () {}),
      ]),
      PlatformMenu(
        label: '窗口',
        menus: <PlatformMenuItem>[
          PlatformMenuItem(
              label: '最小化',
              onSelected: () {
                logic.minimize();
              }),
          PlatformMenuItem(
              label: '最大化',
              onSelected: () {
                logic.maximize();
              }),
        ],
      ),
      PlatformMenu(label: '帮助', menus: <PlatformMenuItem>[
        PlatformMenuItem(label: '报告问题', onSelected: () {}),
      ]),
    ]);
  }
}
