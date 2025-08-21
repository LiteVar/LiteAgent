import 'dart:convert';

import 'package:dio/dio.dart';

import '../../config/constants.dart';
import '../../models/base_response.dart';
import '../../repositories/account_repository.dart';
import '../../utils/log_util.dart';
import '../network/net_util.dart';

class FileServer {
  static Future<BaseResponse<String?>> uploadFile(String filePath) async {
    const String path = '/v1/file/upload';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiServerPath}$path",
      data: FormData.fromMap({"file": await MultipartFile.fromFile(filePath)}),
      options: Options(headers: {'Content-Type': 'multipart/form-data', 'Authorization': token}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }
}
