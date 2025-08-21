import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:lite_agent_client/models/dto/library_page.dart';
import 'package:lite_agent_client/models/dto/retrieval_record_page.dart';
import 'package:lite_agent_client/models/dto/segment.dart';
import 'package:lite_agent_client/models/dto/segment_page.dart';

import '../../config/constants.dart';
import '../../models/base_response.dart';
import '../../models/dto/document_page.dart';
import '../../models/dto/library.dart';
import '../../models/dto/retrieval_result.dart';
import '../../repositories/account_repository.dart';
import '../../repositories/library_repository.dart';
import '../../utils/log_util.dart';
import '../network/net_util.dart';

class LibraryServer {
  static Future<BaseResponse<LibraryPageDto?>> getLibraryList(int pageNo, int pageSize, String? keyWord) async {
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
    return BaseResponse.fromJson(response, (json) => LibraryPageDto.fromJson(json));
  }

  static Future<BaseResponse<DocumentPageDto?>> getDocumentListByLibraryId(String libraryId, int pageNo) async {
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
    return BaseResponse.fromJson(response, (json) => DocumentPageDto.fromJson(json));
  }

  static Future<BaseResponse<SegmentPageDto?>> getSegmentList(String documentId, int pageNo, String? keyWord) async {
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
    return BaseResponse.fromJson(response, (json) => SegmentPageDto.fromJson(json));
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
    return BaseResponse.fromJsonForList(response, (list) {
      return (list).map((json) => SegmentDto.fromJson(json)).toList();
    });
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
    return BaseResponse.fromJsonForList(response, (list) {
      return (list).map((json) => RetrievalResultDto.fromJson(json)).toList();
    });
  }

  static Future<BaseResponse<RetrievalRecordPageDto?>> getRetrievalRecordList(String libraryId, int pageNo) async {
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
    return BaseResponse.fromJson(response, (json) => RetrievalRecordPageDto.fromJson(json));
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
}
