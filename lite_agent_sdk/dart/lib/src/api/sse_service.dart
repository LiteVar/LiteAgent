import 'dart:convert';
import 'dart:io';
import '../model/model.dart';

class SseService {
  String baseUrl;
  String? apiKey;

  SseService(this.baseUrl, this.apiKey);

  Future<Stream<String>> chat(String sessionId, UserTask userTaskDto) async {
    Uri uri = Uri.parse('$baseUrl/chat?sessionId=$sessionId');
    HttpClient httpClient = HttpClient();

    HttpClientRequest request = await httpClient.postUrl(uri);

    if(apiKey != null) request.headers.add("Authorization", 'Bearer $apiKey');
    request.headers.add(HttpHeaders.acceptHeader, 'text/event-stream');
    request.headers.add(HttpHeaders.contentTypeHeader, 'application/json');

    Map<String, dynamic> data = userTaskDto.toJson();

    request.add(utf8.encode(jsonEncode(data)));

    HttpClientResponse response = await request.close();

    Stream<String> stream = response.transform(utf8.decoder);
    return stream;
  }
}