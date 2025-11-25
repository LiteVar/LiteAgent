import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/models/dto/model.dart';

import '../../config/constants.dart';
import '../../models/dto/base/base_response.dart';
import '../../models/dto/base/page.dart';
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

  static Future<BaseResponse<PageDTO<ModelDTO>?>> getAutoAgentModelList() async {
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
    return BaseResponse.fromJson(
        response, (page) => PageDTO<ModelDTO>.fromJson(page, (json) => ModelDTO.fromJson(json as Map<String, dynamic>)));
  }

  static Future<BaseResponse<String?>> importModel(Map<String, dynamic> jsonMap) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    const String path = '/v1/model/add';
    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiServerPath}$path",
      data: jsonMap,
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }

  static Future<BaseResponse<Map<String, String>?>> importModelsByFiles(List<String> filePaths) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    const String path = '/v1/model/import';

    final files = await Future.wait(filePaths.map((p) => MultipartFile.fromFile(p)));
    final formData = FormData.fromMap({'files': files});

    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiServerPath}$path",
      data: formData,
      options: Options(headers: {'Content-Type': 'multipart/form-data', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForMap(response, (map) => Map<String, String>.from(map));
  }
}
