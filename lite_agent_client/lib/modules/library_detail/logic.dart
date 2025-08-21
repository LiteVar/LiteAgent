import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/modules/segment/logic.dart';
import 'package:window_manager/window_manager.dart';

class LibraryDetailLogic extends GetxController with WindowListener {
  static const String PAGE_DOCUMENT = "document";
  static const String PAGE_RETRIEVAL = "retrieval";
  static const String PAGE_SEGMENT = "segment";

  var currentPage = PAGE_DOCUMENT.obs;
  var isFullScreen = false.obs;
  var libraryName = "".obs;
  var libraryId = "";

  @override
  void onInit() {
    var param = Get.arguments;
    if (param is! LibraryDto) {
      Get.back();
      return;
    }
    libraryId = param.id;
    libraryName.value = param.name;

    super.onInit();
    initWindow();
  }

  void initWindow() async {
    checkIsFullScreen();
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  @override
  void onClose() {
    super.onClose();
  }

  @override
  void onWindowResized() async {
    checkIsFullScreen();
    super.onWindowResized();
  }

  @override
  void onWindowClose() {
    Get.back();
  }

  void checkIsFullScreen() async {
    isFullScreen.value = await windowManager.isFullScreen();
  }

  void goBack() {
    Get.back();
  }

  void switchPage(String page) {
    if (page == PAGE_DOCUMENT && Get.isRegistered<SegmentLogic>()) {
      currentPage.value = PAGE_SEGMENT;
      return;
    }
    currentPage.value = page;
  }
}
