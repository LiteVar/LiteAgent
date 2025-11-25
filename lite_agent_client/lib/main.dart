import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:get/get_navigation/src/root/get_material_app.dart';
import 'package:hive_flutter/adapters.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/models/local/conversation.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/models/local/message.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/models/local/thought.dart';
import 'package:lite_agent_client/models/local/tool.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:lite_agent_client/utils/hive_migration_util.dart';
import 'package:path_provider/path_provider.dart';
import 'package:window_manager/window_manager.dart';

import 'config/routes.dart';

Future<void> main() async {
  // Ensure Flutter bindings are initialized
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Log util
  await Log.init();

  // Initialize Hive
  await initHive();

  // Copy server config file
  await copyConfigFile();

  // Initialize window
  await initWindow();

  runApp(const MyApp());
}

Future<void> initHive() async {
  // 在Hive初始化前先进行目录迁移检查
  final hivePath = await HiveMigrationUtil.migrateHiveData();
  
  // 使用迁移后的路径初始化Hive
  await Hive.initFlutter(hivePath);
  
  Hive.registerAdapter(AgentModelAdapter());
  Hive.registerAdapter(ToolModelAdapter());
  Hive.registerAdapter(ModelDataAdapter());
  Hive.registerAdapter(ChatMessageModelAdapter());
  Hive.registerAdapter(ConversationModelAdapter());
  Hive.registerAdapter(ToolFunctionModelAdapter());
  Hive.registerAdapter(ThoughtModelAdapter());
}

Future<void> copyConfigFile() async {
  try {
    final appDir = await getApplicationSupportDirectory();
    final configDir = Directory('${appDir.path}${Platform.pathSeparator}bin');
    if (!configDir.existsSync()) {
      configDir.createSync(recursive: true);
    }

    final configContent = await rootBundle.loadString('bin/config.json');
    final configFile = File('${configDir.path}${Platform.pathSeparator}config.json');
    await configFile.writeAsString(configContent);

    Directory.current = appDir.path;
  } catch (e) {
    Log.e('Copy config file error', e);
  }
}

Future<void> initWindow() async {
  WidgetsFlutterBinding.ensureInitialized();
  await windowManager.ensureInitialized();

  WindowOptions windowOptions = WindowOptions(
    size: const Size(1080, 720),
    minimumSize: const Size(1080, 720),
    center: true,
    backgroundColor: Colors.transparent,
    skipTaskbar: false,
    titleBarStyle: Platform.isWindows ? TitleBarStyle.normal : TitleBarStyle.hidden,
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
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [Locale('zh')],
      locale: const Locale('zh'),
      builder: EasyLoading.init(),
    );
  }
}
