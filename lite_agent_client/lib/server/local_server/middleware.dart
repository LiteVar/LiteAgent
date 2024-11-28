import 'dart:convert';

import 'package:lite_agent_client/server/local_server/util/logger.dart';
import 'package:logging/logging.dart';
import 'package:shelf/shelf.dart';


/// Check 'Authorization' header
Middleware checkAuthorization(String validToken) {
  return (Handler innerHandler) {
    return (Request request) async {
      final authorizationHeader = request.headers['Authorization'];
      final tokenPrefix = "Bearer ";
      if (authorizationHeader == null ||
          authorizationHeader != tokenPrefix + validToken) {
        return Response.unauthorized('Invalid or missing authorization token');
      }
      return innerHandler(request);
    };
  };
}

Middleware logRequest() {
  return (Handler innerHandler) {
    return (Request request) async {
      String method = request.method;
      String uri = request.requestedUri.toString();
      String body = await request.readAsString();
      logger.log(LogModule.http, "logRequest",
          detail: "[$method] $uri $body", level: Level.FINE);
      Request newRequest =
          request.change(body: Stream.value(utf8.encode(body)));
      return innerHandler(newRequest);
    };
  };
}
