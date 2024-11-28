import 'dart:async';
import 'dart:convert';
import 'package:lite_agent_client/server/local_server/util/logger.dart';
import 'package:lite_agent_core_dart/lite_agent_core.dart';
import 'package:logging/logging.dart';
import 'package:shelf/shelf.dart';
import 'package:shelf_web_socket/shelf_web_socket.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'dto.dart';
import 'config.dart';

final agentService = AgentService();

final agentController = AgentController(agentService);

Map<String, WebSocketChannel> webSocketSessions = {};

class AgentController {
  final AgentService agentService;

  AgentController(this.agentService);

  Future<Response> getVersion(Request request) async {
    logger.log(LogModule.http, "Request getVersion");
    VersionDto versionDto = VersionDto(version: config.version);
    logger.log(LogModule.http, "Response getVersion", detail: jsonEncode(versionDto.toJson()));
    return Response.ok(jsonEncode(versionDto.toJson()));
  }

  Future<Response> initChat(Request request) async {
    final payload = await request.readAsString();
    final data = jsonDecode(payload);

    try {
      final CapabilityDto capabilityDto = CapabilityDto.fromJson(data);
      logger.log(LogModule.http, "Request initChat", detail: payload, level: Level.FINEST);
      logger.log(LogModule.http, "Request initChat",
          detail: "baseUrl: ${capabilityDto.llmConfig.baseUrl}, " +
              "model: ${capabilityDto.llmConfig.model}, " +
              "systemMessageLength: ${capabilityDto.systemPrompt.length}," +
              "openSpecListLength: ${capabilityDto.openSpecList.length}");

      SessionDto sessionDto = await agentService.initChat(capabilityDto, listen);

      logger.log(LogModule.http, "Response initChat", detail: jsonEncode(sessionDto.toJson()));
      return Response.ok(jsonEncode(sessionDto.toJson()));
    } on FormatException catch (e) {
      logger.log(LogModule.http, "Response initChat FormatException: ${e}", detail: payload, level: Level.WARNING);
      return Response.badRequest(body: e);
    } catch (e) {
      logger.log(LogModule.http, "Response initChat Exception: ${e}", detail: payload, level: Level.WARNING);
      return Response.internalServerError(body: e);
    }
  }

  FutureOr<Response> chat(Request request) {
    Map<String, String> data = request.url.queryParameters;
    logger.log(LogModule.http, "Request chat", detail: jsonEncode(data));
    final SessionDto sessionDto = SessionDto.fromJson(data);

    return webSocketHandler((WebSocketChannel webSocket) {
      webSocketSessions.addAll({sessionDto.id: webSocket});
      webSocket.stream.listen((message) {
        final payload = message as String;
        if (payload == "ping") {
          logger.log(LogModule.ws, "Receive message", detail: payload, level: Level.FINER);
          WebSocketChannel? webSocketChannel = webSocketSessions[sessionDto.id];
          String pong = "pong";
          logger.log(LogModule.ws, "Send message", detail: pong, level: Level.FINER);
          webSocketChannel?.sink.add(pong);
        } else {
          final data = jsonDecode(payload);
          UserTaskDto userTaskDto = UserTaskDto.fromJson(data);
          logger.log(LogModule.ws, "Receive message", detail: payload);
          // try {
          agentService.startChat(sessionDto.id, userTaskDto);
          // } on FormatException catch (e) {
          //   logger.log(LogModule.ws, "Push startChat FormatException: ${e}", detail: jsonEncode(data), level: Level.WARNING);
          //   pushException(sessionDto.id, ExceptionMessageDto(code: 500, message: e.message));
          // } catch(e) {
          //   logger.log(LogModule.ws, "Push startChat Exception: ${e}", detail: e.toString(), level: Level.WARNING);
          //   pushException(sessionDto.id, ExceptionMessageDto(code: 500, message: e.toString()));
          // }
        }
      }, onDone: () {
        logger.log(LogModule.ws, "onDone", detail: jsonEncode(data));
      });
    })(request);
  }

  Future<Response> history(Request request) async {
    final data = await request.url.queryParameters;
    logger.log(LogModule.http, "Request history", detail: jsonEncode(data));
    try {
      final SessionDto sessionDto = SessionDto.fromJson(data);

      List<AgentMessageDto> agentMessageDtoList = await agentService.getHistory(sessionDto.id);
      logger.log(LogModule.http, "Response get history", detail: "agent message list size: ${agentMessageDtoList.length}");
      return Response.ok(jsonEncode(sessionDto.toJson()));
    } on FormatException catch (e) {
      logger.log(LogModule.http, "Response history FormatException: ${e}", detail: jsonEncode(data), level: Level.WARNING);
      return Response.badRequest(body: e);
    } catch (e) {
      logger.log(LogModule.http, "Response history Exception: ${e}", detail: jsonEncode(data), level: Level.WARNING);
      return Response.internalServerError(body: e);
    }
  }

  Future<Response> stopChat(Request request) async {
    final data = await request.url.queryParameters;
    logger.log(LogModule.http, "Request stopChat", detail: jsonEncode(data));
    try {
      final SessionTaskDto sessionTaskDto = SessionTaskDto.fromJson(data);

      await agentService.stopChat(sessionTaskDto);
      logger.log(LogModule.http, "Response stopChat", detail: jsonEncode(sessionTaskDto.toJson()));
      return Response.ok(jsonEncode(sessionTaskDto.toJson()));
    } on FormatException catch (e) {
      logger.log(LogModule.http, "Response stopChat FormatException: ${e}", detail: jsonEncode(data), level: Level.WARNING);
      return Response.badRequest(body: e);
    } on Exception catch (e) {
      logger.log(LogModule.http, "Response stopChat Exception: ${e}", detail: jsonEncode(data), level: Level.WARNING);
      return Response.internalServerError(body: e);
    }
  }

  Future<Response> clearChat(Request request) async {
    final data = await request.url.queryParameters;
    logger.log(LogModule.http, "Request clearChat", detail: jsonEncode(data));
    try {
      final SessionDto sessionDto = SessionDto.fromJson(data);

      await agentService.clearChat(sessionDto.id);
      logger.log(LogModule.http, "Response clearChat", detail: jsonEncode(sessionDto.toJson()));
      return Response.ok(jsonEncode(sessionDto.toJson()));
    } on FormatException catch (e) {
      logger.log(LogModule.http, "Response clearChat FormatException: ${e}", detail: jsonEncode(data), level: Level.WARNING);
      return Response.badRequest(body: e);
    } on Exception catch (e) {
      logger.log(LogModule.http, "Response clearChat Exception: ${e}", detail: jsonEncode(data), level: Level.WARNING);
      return Response.internalServerError(body: e);
    }
  }

  void listen(String sessionId, AgentMessage agentMessage) {
    WebSocketChannel? webSocketChannel = webSocketSessions[sessionId];
    AgentMessageDto agentMessageDto = AgentMessageDto.fromModel(sessionId, agentMessage);
    logger.log(LogModule.ws, "Send message", detail: jsonEncode(agentMessageDto.toJson()));
    webSocketChannel?.sink.add(jsonEncode(agentMessageDto.toJson()));
  }

// void pushException(String sessionId, ExceptionMessageDto exceptionMessageDto) {
//   WebSocketChannel? webSocketChannel = webSocketSessions[sessionId];
//   webSocketChannel?.sink.add(jsonEncode(exceptionMessageDto.toJson()));
// }
}
