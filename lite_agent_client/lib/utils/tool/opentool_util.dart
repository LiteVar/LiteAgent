import 'package:opentool_dart/opentool_dart.dart';

class OpenToolFetcher {
  static Future<OpenTool?> loadByAddress(
      {bool isSSL = false, required String host, required int port, String? apiKey, String? prefix}) async {
    final client = OpenToolClient(isSSL: isSSL, host: host, port: port, apiKey: apiKey);
    return await client.load();
  }

  static Future<ToolReturn> callByAddress({
    bool isSSL = false,
    required String host,
    required int port,
    required FunctionCall functionCall,
    String? apiKey,
    String? prefix,
  }) async {
    final client = OpenToolClient(isSSL: isSSL, host: host, port: port, apiKey: apiKey);
    return await client.call(functionCall);
  }
}
