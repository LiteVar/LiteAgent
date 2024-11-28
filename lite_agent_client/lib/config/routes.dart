import 'package:get/get.dart';
import 'package:lite_agent_client/modules/adjustment/view.dart';
import 'package:lite_agent_client/modules/agent/view.dart';
import 'package:lite_agent_client/modules/chat/view.dart';
import 'package:lite_agent_client/modules/model/view.dart';
import 'package:lite_agent_client/modules/tool/view.dart';

import '../modules/home/view.dart';

class Routes {
  static const home = '/';
  static const chat = '/chat';
  static const agent = '/agent';
  static const tool = '/tool';
  static const model = '/model';
  static const adjustment = '/adjustment';
}

List<GetPage> getPages = [
  GetPage(
    name: Routes.home,
    page: () => HomePage(),
  ),
  GetPage(
    name: Routes.chat,
    page: () => ChatPage(),
  ),
  GetPage(
    name: Routes.chat,
    page: () => AgentPage(),
  ),
  GetPage(
    name: Routes.tool,
    page: () => ToolPage(),
  ),
  GetPage(
    name: Routes.model,
    page: () => ModelPage(),
  ),
  GetPage(
    name: Routes.adjustment,
    page: () => AdjustmentPage(),
  ),
];
