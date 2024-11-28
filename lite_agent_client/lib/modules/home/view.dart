import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/modules/agent/view.dart';
import 'package:lite_agent_client/modules/chat/view.dart';
import 'package:lite_agent_client/modules/model/view.dart';
import 'package:lite_agent_client/modules/tool/view.dart';

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
          _buildMenu(logic),
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
                        return InkWell(
                            onTap: () => logic.showLoginDialog(false),
                            child: const Column(
                              children: [
                                Icon(Icons.person, color: Colors.white, size: 24.0),
                                Text("未登录", style: TextStyle(fontSize: 12, color: Colors.white))
                              ],
                            ));
                      } else {
                        var defaultImageWidget = Image.asset('assets/images/icon_default_user.png', fit: BoxFit.cover);
                        return SizedBox(
                            height: 24,
                            width: 24,
                            child: Image.network(
                              logic.accountAvatar,
                              fit: BoxFit.cover,
                              loadingBuilder: (context, result, progress) => progress == null ? result : defaultImageWidget,
                              errorBuilder: (context, exception, stackTrace) => defaultImageWidget,
                            ));
                      }
                    }))),
                const SizedBox(height: 40),
                Obx(() => Column(
                      children: [
                        _createFuncItem('聊天', Icons.chat, logic.currentPage.value == HomePageLogic.PAGE_CHAT,
                            () => logic.switchPage(HomePageLogic.PAGE_CHAT)),
                        _createFuncItem('Agents', Icons.android, logic.currentPage.value == HomePageLogic.PAGE_AGENT,
                            () => logic.switchPage(HomePageLogic.PAGE_AGENT)),
                        _createFuncItem('工具', Icons.print, logic.currentPage.value == HomePageLogic.PAGE_TOOL,
                            () => logic.switchPage(HomePageLogic.PAGE_TOOL)),
                        _createFuncItem('大模型', Icons.mode, logic.currentPage.value == HomePageLogic.PAGE_MODEL,
                            () => logic.switchPage(HomePageLogic.PAGE_MODEL))
                      ],
                    )),
                const Spacer(),
                _createFuncItem('设置', Icons.settings, false, () => logic.showSettingDialog()),
              ],
            ),
          ),
          Expanded(child: Obx(() {
            switch (logic.currentPage.value) {
              case HomePageLogic.PAGE_CHAT:
                return ChatPage();
              case HomePageLogic.PAGE_AGENT:
                return AgentPage();
              case HomePageLogic.PAGE_TOOL:
                return ToolPage();
              case HomePageLogic.PAGE_MODEL:
                return ModelPage();
              default:
                return Container();
            }
          }))
        ],
      ),
    );
  }

  Widget _createFuncItem(String title, IconData iconData, bool isSelect, Function()? onTap) {
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
              Icon(iconData, color: itemColor, size: 16),
              const SizedBox(height: 4),
              Text(title, style: TextStyle(color: itemColor, fontSize: 12))
            ],
          )),
        ));
  }

  Widget _buildMenu(HomePageLogic logic) {
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
