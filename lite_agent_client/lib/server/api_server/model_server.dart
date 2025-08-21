import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/modules/model/view.dart';

import '../../config/constants.dart';
import '../../models/base_response.dart';
import '../../models/dto/model_page.dart';
import '../../repositories/account_repository.dart';
import '../../utils/log_util.dart';
import '../network/net_util.dart';

class ModelServer {
  static Future<BaseResponse<String?>> modelSync(List<dynamic> jsonArray) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    const String path = '/dataSync/model';
    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiDesktopServerPath}$path",
      data: jsonArray,
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }

  static Future<BaseResponse<String?>> removeModel(String id) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    String path = '/dataSync/model/$id';
    Map<String, dynamic>? response = await NetUtil.instance.delete(
      "$serverUrl${Constants.apiDesktopServerPath}$path",
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }

  static Future<BaseResponse<ModelPageDto?>> getAutoAgentModelList() async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    String path = '/v1/model/list';
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"pageNo": 0, "autoAgent": true, "pageSize": 100000000},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(response, (json) => ModelPageDto.fromJson(json));
  }
}
