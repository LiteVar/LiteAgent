import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/segment.dart';
import 'package:lite_agent_client/models/dto/segment_page.dart';
import 'package:lite_agent_client/modules/document/logic.dart';

import '../../repositories/library_repository.dart';
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
  var keyWord = "";

  var pageButtonNumberStart = 1;
  final int pageButtonCount = 10;

  @override
  void onInit() {
    super.onInit();
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

    SegmentPageDto? data = await libraryRepository.getSegmentList(documentId, pageNo, keyWord.trim().isEmpty ? null : keyWord);
    int pageSize = LibraryRepository.SEGMENT_PAGE_SIZE;
    if (data != null) {
      int totalItem = int.parse(data.total);
      int totalPage = totalItem % pageSize > 0 ? (totalItem ~/ pageSize) + 1 : totalItem ~/ pageSize;

      totalCount.value = totalItem;
      this.totalPage.value = totalPage;
      segmentList.assignAll(data.list);
    }
  }

  Future<void> searchKeyWord(String keyWord) async {
    this.keyWord = keyWord;
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
}
