import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/base/page.dart';
import 'package:lite_agent_client/models/dto/segment.dart';
import 'package:lite_agent_client/modules/document/logic.dart';
import 'package:lite_agent_client/server/api_server/file_server.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';

import '../../repositories/library_repository.dart';
import '../../widgets/pagination/pagination_controller.dart';
import '../library_detail/logic.dart';

class SegmentLogic extends GetxController {
  var isShowDrawer = false.obs;
  var segmentList = <SegmentDto>[].obs;
  var totalPage = 0.obs;
  var totalCount = 0.obs;
  var currentPage = 1.obs;
  var selectedSegment = Rx<SegmentDto?>(null);
  var documentName = "".obs;
  var documentId = "";
  var fileId = "";
  var keyWord = "".obs;

  var pageButtonNumberStart = 1;
  final int pageButtonCount = 10;

  late final PaginationController paginationController;

  @override
  void onInit() {
    super.onInit();
    initPagination();
    initData();
  }

  void showSegmentDetailDrawer(SegmentDto segment) {
    selectedSegment.value = segment;
    isShowDrawer.value = true;
  }

  void closeSegmentDrawer() {
    isShowDrawer.value = false;
  }

  Future<void> initData() async {
    final DocumentLogic documentLogic = Get.find();
    if (documentLogic.selectedDocument != null) {
      documentName.value = documentLogic.selectedDocument!.name;
      documentId = documentLogic.selectedDocument!.id;
      fileId = documentLogic.selectedDocument!.fileId;
      loadData(1);
    }
  }

  Future<void> loadData(int pageNo) async {
    if (pageNo <= 0 || (totalPage.value != 0 && pageNo > totalPage.value) || documentId.isEmpty) {
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

    PageDTO<SegmentDto>? data = await libraryRepository.getSegmentList(documentId, pageNo, keyWord.value.trim().isEmpty ? null : keyWord.value);
    int pageSize = LibraryRepository.SEGMENT_PAGE_SIZE;
    if (data != null) {
      int totalItem = data.total;
      int totalPage = totalItem % pageSize > 0 ? (totalItem ~/ pageSize) + 1 : totalItem ~/ pageSize;

      totalCount.value = totalItem;
      this.totalPage.value = totalPage;
      segmentList.assignAll(data.list);
    }
  }

  Future<void> searchKeyWord(String keyWord) async {
    this.keyWord.value = keyWord;
    loadData(1);
    /*if (keyWord.isEmpty) {
      await loadData(1);
      return;
    }
    totalPage.value = 0;
    currentPage.value = 1;
    List<SegmentDto> data = await libraryRepository.searchSegmentsByText(documentId, keyWord);
    totalCount.value = data.length;
    segmentList.assignAll(data);*/
  }

  void onInputSubmit(String string) {
    searchKeyWord(string);
  }

  void onGoBackButtonClick() {
    Get.delete<SegmentLogic>();
    LibraryDetailLogic libraryLogic = Get.find();
    libraryLogic.switchPage(LibraryDetailLogic.PAGE_DOCUMENT);
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

  Future<void> downloadDocumentFile() async {
    final DocumentLogic documentLogic = Get.find();
    var document = documentLogic.selectedDocument;
    if (document != null) {
      final result = await FileServer.downloadDatasetMarkdownZip(fileId: fileId);
      switch (result) {
        case DownloadResult.success:
          AlarmUtil.showAlertToast('下载成功');
          break;
        case DownloadResult.cancelled:
          // 用户取消不显示提示，或者显示"已取消"
          break;
        case DownloadResult.failed:
          AlarmUtil.showAlertToast('下载失败');
          break;
      }
    } else {
      AlarmUtil.showAlertToast("文档下载失败,请退出页面重试");
    }
  }
}
