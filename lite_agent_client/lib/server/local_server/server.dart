import 'dart:async';
import 'dart:io';

import 'package:lite_agent_client/server/local_server/router.dart';
import 'package:lite_agent_client/server/local_server/util/logger.dart';
import 'package:shelf/shelf.dart';
import 'package:shelf/shelf_io.dart';
import 'package:shelf_router/shelf_router.dart';

import 'config.dart';
import 'middleware.dart';

void main(List<String> args) async {
  await startServer();
}

Future<void> startServer() async {
  final Router router = Router();
  apiRoutes();
  router.mount(config.server.apiPathPrefix, apiRouter);
  Handler handler = const Pipeline()
      //.addMiddleware(checkAuthorization("<ADD_YOUR_AUTHORIZATION_HERE>"))
      .addMiddleware(logRequest())
      .addHandler(router);

  HttpServer server = await serve(handler, config.server.ip, config.server.port);
  logger.log(LogModule.http, "Start Server", detail: "http://${server.address.host}:${server.port}${config.server.apiPathPrefix}");
}
