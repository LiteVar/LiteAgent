import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/document.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/base/page.dart';

import '../../repositories/library_repository.dart';
import '../../server/api_server/file_server.dart';
import '../../utils/alarm_util.dart';
import '../../widgets/pagination/pagination_controller.dart';
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

  // 分页控制器
  late final PaginationController paginationController;

  @override
  void onInit() {
    super.onInit();
    initPagination();

    var param = Get.arguments as LibraryDto;
    libraryId = param.id;
    initData();
  }

  /// 初始化分页控制器
  void initPagination() {
    paginationController = PaginationController();
    paginationController.initialize(
      currentPage: currentPage.value,
      totalPage: totalPage.value,
      pageButtonCount: pageButtonCount,
      pageButtonNumberStart: pageButtonNumberStart,
      onPageChanged: loadData,
    );

    // 设置双向同步
    ever(currentPage, (page) => paginationController.updateCurrentPage(page));
    ever(totalPage, (totalPage) => paginationController.updateTotalPage(totalPage));
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

    PageDTO<DocumentDto>? data = await libraryRepository.getDocumentListBy(libraryId, pageNo);
    int pageSize = LibraryRepository.DOCUMENT_PAGE_SIZE;
    if (data != null) {
      int totalItem = data.total;
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

  Future<void> download(DocumentDto document) async {
    final result = await FileServer.downloadDatasetMarkdownZip(fileId: document.fileId);
    switch (result) {
      case DownloadResult.success:
        AlarmUtil.showAlertToast('下载成功');
        break;
      case DownloadResult.cancelled:
        break;
      case DownloadResult.failed:
        AlarmUtil.showAlertToast('下载失败');
        break;
    }
  }
}
