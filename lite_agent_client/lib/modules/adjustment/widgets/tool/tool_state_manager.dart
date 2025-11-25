import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/config/constants.dart';

/// 工具状态管理器
/// 负责管理工具相关的所有状态数据
class ToolStateManager {
  // 工具列表数据
  final RxList<ToolFunctionModel> functionList = <ToolFunctionModel>[].obs;

  // UI 状态
  final RxBool isToolExpanded = false.obs;
  final RxBool showMoreTool = false.obs;
  final RxString toolHoverItemId = "".obs;
  final RxInt toolOperationMode = OperationMode.PARALLEL.obs;

  void addFunction(ToolFunctionModel function) {
    functionList.add(function);
  }

  void removeFunction(int index) {
    if (index >= 0 && index < functionList.length) {
      functionList.removeAt(index);
    }
  }

  void clearFunctions() {
    functionList.clear();
  }

  void toggleToolExpanded(bool expanded) {
    isToolExpanded.value = expanded;
  }

  void toggleShowMoreTool(bool showMore) {
    showMoreTool.value = showMore;
  }

  void hoverToolItem(String id) {
    toolHoverItemId.value = id;
  }

  void updateToolOperationMode(int mode) {
    toolOperationMode.value = mode;
  }

  // 获取当前工具列表
  List<ToolFunctionModel> get currentFunctionList => functionList;

  // 获取工具数量
  int get functionCount => functionList.length;

  // 检查是否有工具
  bool get hasFunctions => functionList.isNotEmpty;

  // 检查是否应该显示"更多"按钮
  bool get shouldShowMoreButton => functionList.length > 2;

  // 获取显示的工具数量（考虑"更多"状态）
  int get displayFunctionCount {
    if (!showMoreTool.value && functionList.length > 2) {
      return 2;
    }
    return functionList.length;
  }
}
