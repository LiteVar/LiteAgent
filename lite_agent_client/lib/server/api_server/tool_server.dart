import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/models/base_response.dart';
import 'package:lite_agent_client/models/dto/tool.dart';
import 'package:lite_agent_client/models/dto/tool_detail.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/server/network/net_util.dart';

class ToolServer {
  static Future<BaseResponse<List<ToolDTO>?>> getTooList(int tab) async {
    const String path = '/v1/tool/list';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic> response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"tab": tab},
      options: Options(
        headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId},
      ),
    );
    print("response${jsonEncode(response)}");
    return BaseResponse.fromJsonForList(response, (list) {
      return (list).map((json) => ToolDTO.fromJson(json)).toList();
    });
  }

  static Future<BaseResponse<ToolDetailDTO?>> getToolDetail(String id) async {
    const String path = '/v1/tool/detail';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic> response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path/$id",
      options: Options(
        headers: {'Content-Type': 'application/json', 'Authorization': token},
      ),
    );
    print("response${jsonEncode(response)}");
    return BaseResponse.fromJson(response, (json) => ToolDetailDTO.fromJson(json));
  }
}
