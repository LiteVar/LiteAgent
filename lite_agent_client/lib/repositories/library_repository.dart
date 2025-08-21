import 'package:lite_agent_client/models/dto/document_page.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/retrieval_record_page.dart';
import 'package:lite_agent_client/models/dto/segment.dart';
import 'package:lite_agent_client/models/dto/segment_page.dart';
import 'package:lite_agent_client/server/api_server/library_server.dart';

import '../models/dto/library_page.dart';
import '../models/dto/retrieval_result.dart';

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

  Future<LibraryPageDto?> getLibraryList(int pageNo, String? keyWord) async {
    var response = await LibraryServer.getLibraryList(pageNo, LibraryRepository.LIBRARY_PAGE_SIZE, keyWord);
    return response.data;
  }

  Future<DocumentPageDto?> getDocumentListBy(String libraryId, int pageNo) async {
    var response = await LibraryServer.getDocumentListByLibraryId(libraryId, pageNo);
    return response.data;
  }

  Future<SegmentPageDto?> getSegmentList(String documentId, int pageNo, String? keyWord) async {
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

  Future<RetrievalRecordPageDto?> getRetrievalRecordList(String libraryId, int pageNo) async {
    var response = await LibraryServer.getRetrievalRecordList(libraryId, pageNo);
    return response.data;
  }
}
