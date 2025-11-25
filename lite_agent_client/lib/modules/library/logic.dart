import 'dart:async';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/models/dto/base/page.dart';
import 'package:window_manager/window_manager.dart';

import '../../config/routes.dart';
import '../../repositories/library_repository.dart';
import '../../utils/event_bus.dart';
import '../../widgets/pagination/pagination_controller.dart';

class LibraryLogic extends GetxController with WindowListener {
  var libraryList = <LibraryDto>[].obs;
  var totalPage = 0.obs;
  var currentPage = 1.obs;
  var keyWord = "";
  late StreamSubscription _subscription;

  var pageButtonNumberStart = 1;
  final int pageButtonCount = 10;

  // 分页控制器
  late final PaginationController paginationController;

  @override
  void onInit() {
    super.onInit();
    initWindow();
    initPagination();
    initEventBus();
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

    PageDTO<LibraryDto>? data = await libraryRepository.getLibraryList(pageNo, keyWord.trim().isEmpty ? null : keyWord);
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

  void handlePageResultForPage(PageDTO<LibraryDto> data) {
    int pageSize = LibraryRepository.LIBRARY_PAGE_SIZE;
    int totalItem = data.total;
    int totalPage = totalItem % pageSize > 0 ? (totalItem ~/ pageSize) + 1 : totalItem ~/ pageSize;

    this.totalPage.value = totalPage;
  }

  void jumpToDetail(LibraryDto library) {
    Get.toNamed(Routes.libraryDetail, arguments: library);
  }
}
