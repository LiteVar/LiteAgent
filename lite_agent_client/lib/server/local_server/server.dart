import 'dart:io';

import 'package:lite_agent_core_dart/lite_agent_service.dart';
import 'package:lite_agent_core_dart_server/lite_agent_core_dart_server.dart';
import 'package:opentool_dart/opentool_dart.dart';
import 'package:shelf/shelf.dart';
import 'package:shelf/shelf_io.dart';
import 'package:shelf_router/shelf_router.dart';
import 'package:dart_openai_sdk/dart_openai_sdk.dart';

import '../../utils/log_util.dart' as client_log;

/// 全局工具驱动列表，可供所有代理会话使用
final List<ToolDriver> globalToolDriverList = [
  /// 在此处添加全局工具驱动
];

/// 开放工具驱动映射，用于OpenTool规范使用
final Map<OpenToolDto, OpenToolDriver> openToolDriverInfoMap = {
  /// 在此处添加OpenTool驱动映射
  // OpenToolDto(openToolId: "mock-tool", description: "A Mock CRUD Sample Tools"): MockDriver()
};

/// 全局代理服务实例
final AgentService agentService = AgentService(globalToolDriverList: globalToolDriverList);

/// 全局代理管理服务实例
late final AgentManageService agentManageService;

void main(List<String> args) async {
  await startServer();
}

Future<void> startServer() async {
  await killProcessUsingPort(config.server.port);

  OpenAI.showLogs = false;

  String? dbPath = config.server.dbPath;
  agentManageService = AgentManageServiceByDB(
    AgentDatabaseFactory.getInstance(dbPath != null ? File(dbPath) : null)
  );

  final AgentController agentController = AgentController(
    agentService, 
    agentManageService, 
    openToolDriverInfoMap: openToolDriverInfoMap
  );
  
  final AgentManageController agentManageController = AgentManageController(agentManageService);

  apiRoutes(agentController, agentManageController);

  final Router mainRouter = Router();
  mainRouter.mount(config.server.apiPathPrefix, apiRouter);
  Handler handler = const Pipeline()
      //.addMiddleware(checkAuthorization("<ADD_YOUR_AUTHORIZATION_HERE>"))
      .addMiddleware(logRequest())
      .addHandler(mainRouter);

  // 启动HTTP服务器
  HttpServer server = await serve(handler, config.server.ip, config.server.port);
  logger.log(
    LogModule.http, 
    "服务器已启动", 
    detail: "http://${server.address.host}:${server.port}${config.server.apiPathPrefix}"
  );
}

/// 终止占用指定端口的进程
///
/// @param port 要检查的端口号
Future<void> killProcessUsingPort(int port) async {
  try {
    String command;
    if (Platform.isWindows) {
      command = 'netstat -ano | findstr :$port';
    } else {
      command = 'lsof -i :$port';
    }

    ProcessResult result = await Process.run('bash', ['-c', command]);

    if (result.exitCode == 0) {
      String output = result.stdout;
      List<String> lines = output.split('\n');
      for (String line in lines) {
        if (line.isNotEmpty) {
          List<String> parts = line.trim().split(RegExp(r'\s+'));
          String pid = Platform.isWindows ? parts.last : parts[1];

          if (Platform.isWindows) {
            await Process.run('taskkill', ['/F', '/PID', pid]);
          } else {
            await Process.run('kill', ['-9', pid]);
          }
          client_log.Log.i('Terminated process with PID: $pid');
        }
      }
    } else {
      client_log.Log.i('No process found using port $port');
    }
  } catch (e) {
    client_log.Log.e('Error terminating process: $e');
  }
}