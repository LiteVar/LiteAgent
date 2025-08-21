import 'dart:async';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/library_page.dart';
import 'package:window_manager/window_manager.dart';

import '../../config/routes.dart';
import '../../repositories/library_repository.dart';
import '../../utils/event_bus.dart';

class LibraryLogic extends GetxController with WindowListener {
  var libraryList = <LibraryDto>[].obs;
  var totalPage = 0.obs;
  var currentPage = 1.obs;
  var keyWord = "";
  late StreamSubscription _subscription;

  var pageButtonNumberStart = 1;
  final int pageButtonCount = 10;

  @override
  void onInit() {
    super.onInit();
    initWindow();

    initEventBus();
    initData();
  }

  void initWindow() async {
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  @override
  void onClose() {
    _subscription.cancel();
    super.onClose();
  }

  @override
  void onWindowClose() {
    Get.back();
  }

  Future<void> refreshList() async {
    if (!(await loadData(currentPage.value))) {
      if (currentPage.value > 1) {
        currentPage.value -= 1;
        await refreshList();
      }
    }
  }

  Future<void> initData() async {
    loadData(1);
  }

  Future<bool> loadData(int pageNo) async {
    if (pageNo <= 0 || (totalPage.value != 0 && pageNo > totalPage.value)) {
      return false;
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

    LibraryPageDto? data = await libraryRepository.getLibraryList(pageNo, keyWord.trim().isEmpty ? null : keyWord);
    if (data != null) {
      handlePageResultForPage(data);
      libraryList.assignAll(data.list);
      return data.list.isNotEmpty;
    }
    return false;
  }

  void initEventBus() {
    _subscription = eventBus.on<MessageEvent>().listen((event) {
      if (event.message == EventBusMessage.login) {
        initData();
      } else if (event.message == EventBusMessage.logout) {
        libraryList.clear();
      }
    });
  }

  Future<void> searchKeyWord(String string) async {
    keyWord = string;
    loadData(1);
  }

  void handlePageResultForPage(LibraryPageDto data) {
    int pageSize = LibraryRepository.LIBRARY_PAGE_SIZE;
    int totalItem = int.parse(data.total);
    int totalPage = totalItem % pageSize > 0 ? (totalItem ~/ pageSize) + 1 : totalItem ~/ pageSize;

    this.totalPage.value = totalPage;
  }

  void jumpToDetail(LibraryDto library) {
    Get.toNamed(Routes.libraryDetail, arguments: library);
  }
}
