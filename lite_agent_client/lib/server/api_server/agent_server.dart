import 'dart:convert';

import 'package:dio/dio.dart';

import '../../config/constants.dart';
import '../../models/base_response.dart';
import '../../models/dto/agent.dart';
import '../../models/dto/agent_detail.dart';
import '../../models/dto/chat.dart';
import '../../repositories/account_repository.dart';
import '../network/net_util.dart';

class AgentServer {
  static Future<BaseResponse<List<AgentDTO>?>> getAgentList(int tab) async {
    const String path = '/v1/agent/list';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"tab": tab},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    print("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForList(response, (list) {
      return (list).map((json) => AgentDTO.fromJson(json)).toList();
    });
  }

  static Future<BaseResponse<AgentDetailDTO?>> getAgentDetail(String id) async {
    String path = '/dataSync/agent/$id';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiDesktopServerPath}$path",
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    print("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(response, (json) => AgentDetailDTO.fromJson(json));
  }

  static Future<BaseResponse<List<ChatDTO>?>> getAgentConversationList() async {
    const String path = '/v1/chat/recentAgent';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    print("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForList(response, (list) {
      return (list).map((json) => ChatDTO.fromJson(json)).toList();
    });
  }

  static Future<BaseResponse<String?>> agentSync(List<dynamic> jsonArray) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    const String path = '/dataSync/agent';
    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiDesktopServerPath}$path",
      data: jsonArray,
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token}),
    );
    print("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }

  static Future<BaseResponse<String?>> removeAgent(String id) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    String path = '/dataSync/agent/$id';
    Map<String, dynamic>? response = await NetUtil.instance.delete(
      "$serverUrl${Constants.apiDesktopServerPath}$path",
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token}),
    );
    print("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }
}
