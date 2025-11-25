import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/library.dart';

/// 知识库状态管理器
/// 负责管理知识库相关的所有状态数据
class LibraryStateManager {
  // 知识库列表数据
  final RxList<LibraryDto> libraryList = <LibraryDto>[].obs;

  // UI 状态
  final RxBool isLibraryExpanded = false.obs;
  final RxBool showMoreLibrary = false.obs;
  final RxString libraryHoverItemId = "".obs;

  void addLibrary(LibraryDto library) {
    libraryList.add(library);
  }

  void removeLibrary(int index) {
    if (index >= 0 && index < libraryList.length) {
      libraryList.removeAt(index);
    }
  }

  void clearLibraries() {
    libraryList.clear();
  }

  void toggleLibraryExpanded(bool expanded) {
    isLibraryExpanded.value = expanded;
  }

  void toggleShowMoreLibrary(bool showMore) {
    showMoreLibrary.value = showMore;
  }

  void hoverLibraryItem(String id) {
    libraryHoverItemId.value = id;
  }

  // 获取当前知识库列表
  List<LibraryDto> get currentLibraryList => libraryList;

  // 获取知识库数量
  int get libraryCount => libraryList.length;

  // 检查是否有知识库
  bool get hasLibraries => libraryList.isNotEmpty;

  // 检查是否应该显示"更多"按钮
  bool get shouldShowMoreButton => libraryList.length > 2;

  // 获取显示的知识库数量（考虑"更多"状态）
  int get displayLibraryCount {
    if (!showMoreLibrary.value && libraryList.length > 2) {
      return 2;
    }
    return libraryList.length;
  }
}
