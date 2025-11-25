import 'package:lite_agent_client/models/dto/document.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/local/library_upload_result.dart';
import 'package:lite_agent_client/models/dto/base/page.dart';
import 'package:lite_agent_client/models/dto/retrieval_record.dart';
import 'package:lite_agent_client/models/dto/retrieval_result.dart';
import 'package:lite_agent_client/models/dto/segment.dart';
import 'package:lite_agent_client/server/api_server/library_server.dart';

import '../models/dto/base/base_response.dart';

final libraryRepository = LibraryRepository();

class LibraryRepository {
  static final LibraryRepository _instance = LibraryRepository._internal();

  factory LibraryRepository() => _instance;

  LibraryRepository._internal();

  static const LIBRARY_PAGE_SIZE = 12;
  static const DOCUMENT_PAGE_SIZE = 10;
  static const SEGMENT_PAGE_SIZE = 10;
  static const RECORD_PAGE_SIZE = 10;

  Future<LibraryDto?> getLibraryById(String libraryId) async {
    var response = await LibraryServer.getLibraryById(libraryId);
    return response.data;
  }

  Future<List<LibraryDto>?> getLibraryListForSelectDialog() async {
    var response = await LibraryServer.getLibraryList(1, 10000, null);
    return response.data?.list;
  }

  Future<bool> checkIsLibraryListEmpty() async {
    var response = await LibraryServer.getLibraryList(1, 1, null);
    if (response.data != null) {
      return response.data!.list.isEmpty;
    }
    return false;
  }

  Future<PageDTO<LibraryDto>?> getLibraryList(int pageNo, String? keyWord) async {
    var response = await LibraryServer.getLibraryList(pageNo, LibraryRepository.LIBRARY_PAGE_SIZE, keyWord);
    return response.data;
  }

  Future<PageDTO<DocumentDto>?> getDocumentListBy(String libraryId, int pageNo) async {
    var response = await LibraryServer.getDocumentListByLibraryId(libraryId, pageNo);
    return response.data;
  }

  Future<PageDTO<SegmentDto>?> getSegmentList(String documentId, int pageNo, String? keyWord) async {
    var response = await LibraryServer.getSegmentList(documentId, pageNo, keyWord);
    return response.data;
  }

  Future<List<SegmentDto>> searchSegmentsByText(String documentId, String inputText) async {
    List<SegmentDto> list = [];
    var response = await LibraryServer.searchSegmentsByText(documentId, inputText);
    if (response.data != null) {
      list.addAll(response.data!);
    }
    return list;
  }

  Future<List<RetrievalResultDto>> retrieveTest(String libraryId, String inputText) async {
    List<RetrievalResultDto> list = [];
    var response = await LibraryServer.retrieve(libraryId, inputText);
    if (response.data != null) {
      list.addAll(response.data!);
    }
    return list;
  }

  Future<PageDTO<RetrievalRecordDto>?> getRetrievalRecordList(String libraryId, int pageNo) async {
    var response = await LibraryServer.getRetrievalRecordList(libraryId, pageNo);
    return response.data;
  }

  Future<List<RetrievalResultDto>> getRetrieveHistory(String historyId) async {
    List<RetrievalResultDto> list = [];
    var response = await LibraryServer.getRetrievalRecordHistoryList(historyId);
    if (response.data != null) {
      list.addAll(response.data!);
    }
    return list;
  }

  Future<String?> getDocumentPreview(String fileId) async {
    var response = await LibraryServer.getDocumentPreview(fileId);
    return response?.data;
  }

  Future<LibraryUploadResult?> uploadLibraryZip(String filePath) async {
    var response = await LibraryServer.uploadLibraryZip(filePath);
    final data = response?.data;
    if (data == null) {
      return null;
    }

    final token = data['token'] as String?;
    final modelMap = data['modelMap'] as Map<String, dynamic>?;
    final knowledgeBaseMap = data['knowledgeBaseMap'] as Map<String, dynamic>?;

    return LibraryUploadResult(token: token, modelMap: modelMap, knowledgeBaseMap: knowledgeBaseMap);
  }

  Future<BaseResponse<Map<String, String>?>?> saveImportData(LibraryUploadResult result) async {
    if (result.token == null) {
      return null;
    }
    final response = await LibraryServer.saveImportData(
      token: result.token ?? '',
      modelMap: result.modelMap ?? {},
      knowledgeBaseMap: result.knowledgeBaseMap ?? {},
    );
    return response;
  }

  Future<bool> exportKnowledge({required List<String> datasetIds, required String savePath, bool plainText = false}) async {
    return await LibraryServer.exportKnowledge(datasetIds: datasetIds, savePath: savePath, plainText: plainText);
  }
}
