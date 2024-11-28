import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/models/base_response.dart';
import 'package:lite_agent_client/models/dto/workspace.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/server/network/net_util.dart';

class WorkSpaceServer {
  static Future<BaseResponse<List<WorkSpaceDTO>?>> getWorkspaceList() async {
    const String path = '/v1/workspace/list';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic> response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      options: Options(
        headers: {'Content-Type': 'application/json', 'Authorization': token},
      ),
    );
    print("response${jsonEncode(response)}");
    return BaseResponse.fromJsonForList(response, (list) {
      return (list).map((json) => WorkSpaceDTO.fromJson(json)).toList();
    });
  }
}
