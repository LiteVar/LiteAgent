import 'package:flutter/cupertino.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/retrieval_record.dart';
import 'package:lite_agent_client/models/dto/retrieval_result.dart';

import '../../repositories/library_repository.dart';
import '../../utils/alarm_util.dart';

class RetrievalLogic extends GetxController {
  final TextEditingController retrieveController = TextEditingController();
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

  @override
  void onInit() {
    super.onInit();
    retrieveController.addListener(_onTextChange);

    var param = Get.arguments as LibraryDto;
    libraryId = param.id;

    loadRetrievalRecord(1);
  }

  @override
  void onClose() {
    retrieveController.removeListener(_onTextChange);
    retrieveController.dispose();
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
      int totalItem = int.parse(data.total);
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
  }

  void _onTextChange() {
    textCount.value = retrieveController.text.length;
    enableRetrieval.value = retrieveController.text.trim().isNotEmpty;
  }

  void copyRecordTextToInputWidget(String text) {
    retrieveController.text = text;
  }
}
