import 'package:flutter/cupertino.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/retrieval_record.dart';
import 'package:lite_agent_client/models/dto/retrieval_result.dart';

import '../../repositories/library_repository.dart';
import '../../server/api_server/file_server.dart';
import '../../utils/alarm_util.dart';
import '../../widgets/dialog/dialog_markdown_preview.dart';
import '../../widgets/pagination/pagination_controller.dart';

class RetrievalLogic extends GetxController {
  final TextEditingController retrieveController = TextEditingController();
  final ScrollController resultScrollController = ScrollController();
  String libraryId = "";
  var textCount = 0.obs;
  var totalPage = 0.obs;
  var currentPage = 1.obs;
  var resultList = <RetrievalResultDto>[].obs;
  var recordList = <RetrievalRecordDto>[].obs;

  var enableRetrieval = false.obs;
  var showNoResultTips = false.obs;
  var recordHoverItemId = "".obs;
  var pageButtonNumberStart = 1;
  final int pageButtonCount = 5;

  late final PaginationController paginationController;

  @override
  void onInit() {
    super.onInit();
    retrieveController.addListener(_onTextChange);
    initPagination();

    var param = Get.arguments as LibraryDto;
    libraryId = param.id;

    loadRetrievalRecord(1);
  }

  @override
  void onClose() {
    retrieveController.removeListener(_onTextChange);
    retrieveController.dispose();
    resultScrollController.dispose();
    super.onClose();
  }

  Future<void> loadRetrievalRecord(int pageNo) async {
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

    var data = await libraryRepository.getRetrievalRecordList(libraryId, pageNo);
    int pageSize = LibraryRepository.RECORD_PAGE_SIZE;
    if (data != null) {
      /*if (pageNo == 1) {
        recordList.assignAll(data.list);
      } else {
        recordList.addAll(data.list);
      }*/
      int totalItem = data.total;
      int totalPage = totalItem % pageSize > 0 ? (totalItem ~/ pageSize) + 1 : totalItem ~/ pageSize;

      this.totalPage.value = totalPage;

      recordList.assignAll(data.list);
    }
  }

  Future<void> startRetrieval() async {
    if (!enableRetrieval.value) {
      return;
    }
    if (libraryId.isEmpty) {
      return;
    }
    String inputText = retrieveController.text.trim();
    if (inputText.isEmpty) {
      AlarmUtil.showAlertToast("请输入检索文本");
      return;
    }
    EasyLoading.show(status: '正在检索中...');
    var data = await libraryRepository.retrieveTest(libraryId, inputText);
    resultList.assignAll(data);
    showNoResultTips.value = resultList.isEmpty;
    EasyLoading.dismiss();
    loadRetrievalRecord(currentPage.value);
    // 滚动到列表顶部
    _scrollToTop();
  }

  void _scrollToTop() {
    // 立即尝试
    if (resultScrollController.hasClients) {
      resultScrollController.jumpTo(0);
    }

    // 延迟一帧后再次尝试
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (resultScrollController.hasClients) {
        resultScrollController.jumpTo(0);
      }
    });
  }

  void _onTextChange() {
    textCount.value = retrieveController.text.length;
    enableRetrieval.value = retrieveController.text.trim().isNotEmpty;
  }

  /// 初始化分页控制器
  void initPagination() {
    paginationController = PaginationController();
    paginationController.initialize(
      currentPage: currentPage.value,
      totalPage: totalPage.value,
      pageButtonCount: pageButtonCount,
      pageButtonNumberStart: pageButtonNumberStart,
      onPageChanged: loadRetrievalRecord,
    );
    // 设置双向同步
    ever(currentPage, (page) => paginationController.updateCurrentPage(page));
    ever(totalPage, (totalPage) => paginationController.updateTotalPage(totalPage));
  }

  void copyRecordTextToInputWidget(String text) {
    retrieveController.text = text;
  }

  Future<void> getRetrieveHistory(String historyId) async {
    var data = await libraryRepository.getRetrieveHistory(historyId);
    resultList.assignAll(data);
    showNoResultTips.value = resultList.isEmpty;
    // 滚动到列表顶部
    _scrollToTop();
  }

  Future<void> showDocumentDetail(String fileId, String fileName) async {
    String preview = await libraryRepository.getDocumentPreview(fileId) ?? "";
    if (preview.isNotEmpty) {
      Get.dialog(
        barrierDismissible: false,
        MarkdownPreviewDialog(titleText: fileName, contentText: preview),
      );
    }
  }

  Future<void> downloadDocumentFile(String fileId) async {
    final result = await FileServer.downloadDatasetSourceFile(fileId: fileId);
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
