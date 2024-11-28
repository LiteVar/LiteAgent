import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get_navigation/src/root/get_material_app.dart';
import 'package:hive_flutter/adapters.dart';
import 'package:lite_agent_client/models/local_data_model.dart';
import 'package:lite_agent_client/repositories/conversation_repository.dart';
import 'package:window_manager/window_manager.dart';

import 'config/routes.dart';

Future<void> main() async {
  // Ensure Flutter bindings are initialized
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Hive
  await initHive();

  // Initialize window
  await initWindow();

  runApp(const MyApp());
}

Future<void> initHive() async {
  await Hive.initFlutter();
  Hive.registerAdapter(AgentBeanAdapter());
  Hive.registerAdapter(ToolBeanAdapter());
  Hive.registerAdapter(ModelBeanAdapter());
  Hive.registerAdapter(ChatMessageAdapter());
  Hive.registerAdapter(AgentConversationBeanAdapter());
}

Future<void> initWindow() async {
  WidgetsFlutterBinding.ensureInitialized();
  await windowManager.ensureInitialized();

  WindowOptions windowOptions = const WindowOptions(
    size: Size(1080, 720),
    minimumSize: Size(1080, 720),
    center: true,
    backgroundColor: Colors.transparent,
    skipTaskbar: false,
    titleBarStyle: TitleBarStyle.hidden,
    windowButtonVisibility: true,
  );
  windowManager.waitUntilReadyToShow(windowOptions, () async {
    await windowManager.show();
    await windowManager.focus();
  });
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      getPages: getPages,
      initialRoute: Routes.home,
      theme: ThemeData(useMaterial3: true, brightness: Brightness.light),
      darkTheme: ThemeData(useMaterial3: true, brightness: Brightness.light),
      themeMode: ThemeMode.system,
      locale: const Locale('zh'),
      builder: EasyLoading.init(),
    );
  }
}