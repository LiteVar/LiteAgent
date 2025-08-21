import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/document.dart';
import 'package:lite_agent_client/models/dto/document_page.dart';
import 'package:lite_agent_client/models/dto/library.dart';

import '../../repositories/library_repository.dart';
import '../library_detail/logic.dart';

class DocumentLogic extends GetxController {
  var libraryId = "";
  var documentList = <DocumentDto>[].obs;
  var totalPage = 0.obs;
  var totalCount = 0.obs;
  var currentPage = 1.obs;
  DocumentDto? selectedDocument;

  var pageButtonNumberStart = 1;
  final int pageButtonCount = 10;

  @override
  void onInit() {
    super.onInit();

    var param = Get.arguments as LibraryDto;
    libraryId = param.id;
    initData();
  }

  void initData() {
    loadData(1);
  }

  Future<void> loadData(pageNo) async {
    if (libraryId.isEmpty) {
      return;
    }
    if (pageNo <= 0 || (totalPage.value != 0 && pageNo > totalPage.value)) {
      return;
    }
    currentPage.value = pageNo;

    //count pageButtonNumberStart
    if (totalPage.value < pageButtonCount) {
      pageButtonNumberStart = 1;
    } else if (currentPage.value >= pageButtonNumberStart + pageButtonCount) {
      pageButtonNumberStart = currentPage.value - pageButtonCount + 1;
    } else if (currentPage.value <= pageButtonNumberStart) {
      pageButtonNumberStart = currentPage.value;
    }

    DocumentPageDto? data = await libraryRepository.getDocumentListBy(libraryId, pageNo);
    int pageSize = LibraryRepository.DOCUMENT_PAGE_SIZE;
    if (data != null) {
      int totalItem = int.parse(data.total);
      int totalPage = totalItem % pageSize > 0 ? (totalItem ~/ pageSize) + 1 : totalItem ~/ pageSize;

      totalCount.value = totalItem;
      this.totalPage.value = totalPage;
      for (var document in data.list) {
        if (document.htmlUrl != null && document.htmlUrl!.isNotEmpty && document.name.isEmpty) {
          document.name = "链接文档";
        }
      }
      documentList.assignAll(data.list);
    }
  }

  void switchToSegment(DocumentDto document) {
    selectedDocument = document;
    LibraryDetailLogic libraryLogic = Get.find();
    libraryLogic.switchPage(LibraryDetailLogic.PAGE_SEGMENT);
  }
}
