import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_library.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_empty_library.dart';
import 'library_item.dart';
import 'library_state_manager.dart';

/// 知识库列表组件
/// 负责知识库配置的整体展示，包括展开/收起、知识库列表、操作按钮等
class LibraryList extends StatelessWidget {
  final LibraryStateManager libraryStateManager;
  final bool isLogin;
  final VoidCallback? onDataChanged;

  const LibraryList({
    super.key,
    required this.libraryStateManager,
    required this.isLogin,
    this.onDataChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: const EdgeInsets.only(top: 10),
          child: Row(
            children: [
              InkWell(
                onTap: () => libraryStateManager.toggleLibraryExpanded(!libraryStateManager.isLibraryExpanded.value),
                child: Row(
                  children: [
                    Obx(() => buildAssetImage(
                      libraryStateManager.isLibraryExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png",
                      18,
                      const Color(0xff333333),
                    )),
                    const Text("知识库", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
                  ],
                ),
              ),
              const Spacer(),
              buildClickButton("icon_add.png", "添加", _showLibrarySelectDialog),
            ],
          ),
        ),
        const SizedBox(height: 10),
        Obx(() => Offstage(
          offstage: !libraryStateManager.isLibraryExpanded.value,
          child: Column(
            children: [
              Container(
                margin: const EdgeInsets.only(left: 18),
                child: const Text(
                  "在问答中，agent可以引用知识库的内容回答问题",
                  style: TextStyle(color: Color(0xff999999), fontSize: 12),
                ),
              ),
              Obx(() => Container(
                margin: const EdgeInsets.only(top: 10, left: 10),
                child: Column(
                  children: [
                    if (libraryStateManager.hasLibraries)
                      ...List.generate(
                        libraryStateManager.displayLibraryCount,
                        (index) => LibraryItem(
                          library: libraryStateManager.libraryList[index],
                          isHovered: libraryStateManager.libraryHoverItemId.value == index.toString(),
                          onRemove: () => _removeLibrary(index),
                          onHoverEnter: () => libraryStateManager.hoverLibraryItem(index.toString()),
                          onHoverExit: () => libraryStateManager.hoverLibraryItem(""),
                        ),
                      ),
                  ],
                ),
              )),
              Obx(() => Offstage(
                offstage: !libraryStateManager.shouldShowMoreButton,
                child: InkWell(
                  onTap: () => libraryStateManager.toggleShowMoreLibrary(!libraryStateManager.showMoreLibrary.value),
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          libraryStateManager.showMoreLibrary.value ? "收起" : "更多",
                          style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4)),
                        ),
                        const SizedBox(width: 10),
                        buildAssetImage("icon_down.png", 12, const Color(0xff2A82E4)),
                      ],
                    ),
                  ),
                ),
              )),
            ],
          ),
        )),
        horizontalLine(),
      ],
    );
  }

  /// 显示知识库选择对话框
  Future<void> _showLibrarySelectDialog() async {
    if (!isLogin) {
      AlarmUtil.showAlertToast("未登录无法选择线上知识库");
      return;
    }
    if (await libraryRepository.checkIsLibraryListEmpty()) {
      _showEmptyLibraryDialog();
      return;
    }
    var list = libraryStateManager.libraryList.map((library) => library.id).toList();
    Get.dialog(
      barrierDismissible: false,
      SelectLibraryDialog(
        selectLibraryId: list,
        onConfirm: (target) {
          for (var library in libraryStateManager.libraryList) {
            if (target.id == library.id) {
              libraryStateManager.libraryList.remove(library);
              onDataChanged?.call();
              return;
            }
          }
          libraryStateManager.libraryList.add(target);
          onDataChanged?.call();
        },
      ),
    );
  }

  /// 移除知识库
  void _removeLibrary(int index) {
    libraryStateManager.removeLibrary(index);
    onDataChanged?.call();
  }

  /// 显示空知识库对话框
  void _showEmptyLibraryDialog() {
    Get.dialog(barrierDismissible: false, EmptyLibraryDialog());
  }

  Widget buildClickButton(String fileName, String text, Function()? onTap) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 12),
        decoration: const BoxDecoration(
          color: Color(0xffe7f2fe),
          borderRadius: BorderRadius.all(Radius.circular(8)),
        ),
        child: Row(
          children: [
            buildAssetImage(fileName, 14, const Color(0xff2A82E4)),
            const SizedBox(width: 5),
            Text(text, style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4))),
          ],
        ),
      ),
    );
  }
}
