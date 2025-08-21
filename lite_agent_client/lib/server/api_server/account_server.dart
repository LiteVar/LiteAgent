import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/models/base_response.dart';
import 'package:lite_agent_client/models/dto/account.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/server/network/net_util.dart';
import 'package:lite_agent_client/utils/log_util.dart';

class AccountServer {
  static Future<BaseResponse<String?>> login(String email, String password, String server) async {
    const String path = '/v1/auth/login';
    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$server${Constants.apiServerPath}$path",
      data: FormData.fromMap({"email": email, "password": password}),
      options: Options(headers: {'Content-Type': 'application/json'}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }

  static Future<BaseResponse<AccountDTO?>> getUserInfo() async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    const String path = '/v1/user/info';
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(response, (json) => AccountDTO.fromJson(json));
  }

  static Future<BaseResponse<String?>> logout() async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    const String path = '/v1/auth/logout';
    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiServerPath}$path",
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }
}
