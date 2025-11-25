import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/models/dto/base/page.dart';
import 'package:lite_agent_client/models/dto/retrieval_record.dart';
import 'package:lite_agent_client/models/dto/segment.dart';

import '../../config/constants.dart';
import '../../models/dto/base/base_response.dart';
import '../../models/dto/document.dart';
import '../../models/dto/library.dart';
import '../../models/dto/retrieval_result.dart';
import '../../repositories/account_repository.dart';
import '../../repositories/library_repository.dart';
import '../../utils/log_util.dart';
import '../network/net_util.dart';

class LibraryServer {
  static Future<BaseResponse<PageDTO<LibraryDto>?>> getLibraryList(int pageNo, int pageSize, String? keyWord) async {
    const String path = '/v1/dataset/list';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"pageNo": pageNo, "pageSize": pageSize, "query": keyWord},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(
        response, (page) => PageDTO<LibraryDto>.fromJson(page, (json) => LibraryDto.fromJson(json as Map<String, dynamic>)));
  }

  static Future<BaseResponse<PageDTO<DocumentDto>?>> getDocumentListByLibraryId(String libraryId, int pageNo) async {
    String path = '/v1/dataset/$libraryId/documents';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"pageNo": pageNo, "pageSize": LibraryRepository.DOCUMENT_PAGE_SIZE},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(
        response, (page) => PageDTO<DocumentDto>.fromJson(page, (json) => DocumentDto.fromJson(json as Map<String, dynamic>)));
  }

  static Future<BaseResponse<PageDTO<SegmentDto>?>> getSegmentList(String documentId, int pageNo, String? keyWord) async {
    String path = '/v1/dataset/documents/$documentId/segments';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"pageNo": pageNo, "pageSize": LibraryRepository.SEGMENT_PAGE_SIZE, "query": keyWord},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(
        response, (page) => PageDTO<SegmentDto>.fromJson(page, (json) => SegmentDto.fromJson(json as Map<String, dynamic>)));
  }

  static Future<BaseResponse<List<SegmentDto>?>> searchSegmentsByText(String documentId, String inputText) async {
    String path = '/v1/dataset/documents/$documentId/segments/searchByText';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"inputText": inputText},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForList(response, (list) => (list).map((json) => SegmentDto.fromJson(json)).toList());
  }

  static Future<BaseResponse<List<RetrievalResultDto>?>> retrieve(String libraryId, String inputText) async {
    String path = '/v1/dataset/$libraryId/retrieve';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"query": inputText},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForList(response, (list) => (list).map((json) => RetrievalResultDto.fromJson(json)).toList());
  }

  static Future<BaseResponse<PageDTO<RetrievalRecordDto>?>> getRetrievalRecordList(String libraryId, int pageNo) async {
    String path = '/v1/dataset/$libraryId/retrieve/history';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"pageNo": pageNo, "pageSize": LibraryRepository.RECORD_PAGE_SIZE},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(response,
        (page) => PageDTO<RetrievalRecordDto>.fromJson(page, (json) => RetrievalRecordDto.fromJson(json as Map<String, dynamic>)));
  }

  static Future<BaseResponse<LibraryDto?>> getLibraryById(String libraryId) async {
    String path = '/v1/dataset/$libraryId';
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
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(response, (json) => LibraryDto.fromJson(json));
  }

  static Future<BaseResponse<List<RetrievalResultDto>?>> getRetrievalRecordHistoryList(String historyId) async {
    String path = '/v1/dataset/retrieve/history/$historyId';
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
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForList(response, (list) => (list).map((json) => RetrievalResultDto.fromJson(json)).toList());
  }

  static Future<BaseResponse<String>?> getDocumentPreview(String fileId) async {
    String path = '/v1/file/dataset/markdown/preview';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }
    Map<String, dynamic>? response = await NetUtil.instance.get(
      "$serverUrl${Constants.apiServerPath}$path",
      queryParameters: {"fileId": fileId},
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJsonForString(response);
  }

  static Future<BaseResponse<Map<String, dynamic>>?> uploadLibraryZip(String filePath) async {
    const String path = '/desktop/import/preview';
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || token.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }

    FormData formData = FormData.fromMap({'file': await MultipartFile.fromFile(filePath)});

    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiDesktopServerPath}$path",
      data: formData,
      options: Options(headers: {'Content-Type': 'multipart/form-data', 'Authorization': token, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(response, (json) => json);
  }

  static Future<BaseResponse<Map<String, String>?>?> saveImportData(
      {required String token, required Map<String, dynamic> modelMap, required Map<String, dynamic> knowledgeBaseMap}) async {
    String path = '/desktop/import/save/$token';
    String serverUrl = await accountRepository.getApiServerUrl();
    String authToken = await accountRepository.getApiToken();
    String workspaceId = await accountRepository.getWorkSpaceId();
    if (serverUrl.isEmpty || authToken.isEmpty || workspaceId.isEmpty) {
      return BaseResponse(data: null, code: 400, message: "参数缺失");
    }

    Map<String, dynamic> body = {'token': token, 'modelMap': modelMap, 'knowledgeBaseMap': knowledgeBaseMap};

    Map<String, dynamic>? response = await NetUtil.instance.post(
      "$serverUrl${Constants.apiDesktopServerPath}$path",
      data: body,
      options: Options(headers: {'Content-Type': 'application/json', 'Authorization': authToken, 'Workspace-id': workspaceId}),
    );
    Log.d("response:${jsonEncode(response)}");
    return BaseResponse.fromJson(response, (json) => json.map((key, value) => MapEntry(key, value.toString())));
  }

  static Future<bool> exportKnowledge({required List<String> datasetIds, required String savePath, bool plainText = false}) async {
    String serverUrl = await accountRepository.getApiServerUrl();
    String token = await accountRepository.getApiToken();
    if (serverUrl.isEmpty || token.isEmpty) {
      Log.e('导出知识库失败: 参数缺失');
      return false;
    }
    try {
      final dio = Dio();
      await dio.download(
        '$serverUrl${Constants.apiDesktopServerPath}/desktop/import/exportKnowledge',
        savePath,
        queryParameters: {'plainText': plainText, 'datasetIds': datasetIds},
        options: Options(
          responseType: ResponseType.bytes,
          headers: {'accept': 'application/octet-stream,application/zip,application/*,*/*', 'Authorization': token},
        ),
      );
      return true;
    } catch (e) {
      Log.e('导出知识库失败: $e');
      return false;
    }
  }
}
