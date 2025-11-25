import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:file_picker/file_picker.dart';

import '../../config/constants.dart';
import '../../models/dto/base/base_response.dart';
import '../../repositories/account_repository.dart';
import '../../utils/log_util.dart';
import '../network/net_util.dart';

/// 文件下载结果
enum DownloadResult {
  success,   // 下载成功
  failed,    // 下载失败
  cancelled, // 用户取消
}

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

  static Future<DownloadResult> downloadDatasetMarkdownZip({required String fileId}) async {
    final serverUrl = await accountRepository.getApiServerUrl();
    final token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      Log.e('下载文件失败: 参数缺失');
      return DownloadResult.failed;
    }
    final url = '$serverUrl${Constants.apiServerPath}/v1/file/dataset/markdown/download?fileId=$fileId';
    return _downloadFile(url, token);
  }

  static Future<DownloadResult> downloadDatasetSourceFile({required String fileId}) async {
    final serverUrl = await accountRepository.getApiServerUrl();
    final token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      Log.e('下载文件失败: 参数缺失');
      return DownloadResult.failed;
    }
    final url = '$serverUrl${Constants.apiServerPath}/v1/file/dataset/file/download?fileId=$fileId';
    return _downloadFile(url, token);
  }

  /// 通用文件下载方法：从响应头获取文件名，让用户选择保存位置，然后下载
  static Future<DownloadResult> _downloadFile(String url, String token) async {
    try {
      final dio = Dio();
      final headers = {'accept': 'application/octet-stream,application/zip,application/*,*/*', 'Authorization': token};

      // 获取响应头中的文件名
      final response = await dio.head(url, options: Options(headers: headers));
      final fileName = _extractFileNameFromHeaders(response.headers) ?? 'download_file';

      // 让用户选择保存位置
      final savePath = await FilePicker.platform.saveFile(dialogTitle: '选择保存位置', fileName: fileName);
      if (savePath == null) {
        Log.i('用户取消了文件保存');
        return DownloadResult.cancelled;
      }

      // 下载文件
      await dio.download(url, savePath, options: Options(responseType: ResponseType.bytes, headers: headers));
      Log.i('文件已保存: $savePath');
      return DownloadResult.success;
    } catch (e) {
      Log.e('保存文件失败: $e');
      return DownloadResult.failed;
    }
  }

  /// 从响应头中提取文件名
  static String? _extractFileNameFromHeaders(Headers headers) {
    try {
      final contentDisposition = headers.value('content-disposition');
      if (contentDisposition == null) return null;

      // 优先处理 filename*=UTF-8''encoded_filename 格式（RFC 5987）
      final starMatch = RegExp(r"filename\*=UTF-8''([^;]+)", caseSensitive: false).firstMatch(contentDisposition);
      if (starMatch != null) {
        try {
          return Uri.decodeComponent(starMatch.group(1)!);
        } catch (e) {
          Log.e('URL解码文件名失败: $e');
          return starMatch.group(1);
        }
      }

      // 处理 filename="filename.zip" 或 filename=filename.zip 格式
      final quotedMatch = RegExp(r'filename=(["' '])([^"' ']+)\1', caseSensitive: false).firstMatch(contentDisposition);
      if (quotedMatch != null) return quotedMatch.group(2);

      final unquotedMatch = RegExp(r'filename=([^;,\s]+)', caseSensitive: false).firstMatch(contentDisposition);
      return unquotedMatch?.group(1);
    } catch (e) {
      Log.e('提取文件名失败: $e');
      return null;
    }
  }
}
