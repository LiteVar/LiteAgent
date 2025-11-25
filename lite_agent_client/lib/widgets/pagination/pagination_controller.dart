import 'package:get/get.dart';

/// 分页控制器 - 管理分页相关的业务逻辑和状态
class PaginationController extends GetxController {
  /// 当前页码 (从1开始)
  final RxInt _currentPage = 1.obs;

  /// 总页数
  final RxInt _totalPage = 0.obs;

  /// 页码按钮显示数量
  int _pageButtonCount = 5;

  /// 页码按钮起始编号
  int _pageButtonNumberStart = 1;

  /// 页码变更回调函数
  Function(int page)? _onPageChanged;

  // Getters
  RxInt get currentPage => _currentPage;

  RxInt get totalPage => _totalPage;

  int get pageButtonCount => _pageButtonCount;

  int get pageButtonNumberStart => _pageButtonNumberStart;

  /// 初始化分页控制器
  void initialize({
    required int currentPage,
    required int totalPage,
    int pageButtonCount = 10,
    required int pageButtonNumberStart,
    Function(int page)? onPageChanged,
  }) {
    _currentPage.value = currentPage;
    _totalPage.value = totalPage;
    _pageButtonCount = pageButtonCount;
    _pageButtonNumberStart = pageButtonNumberStart;
    _onPageChanged = onPageChanged;
  }

  /// 更新当前页码
  void updateCurrentPage(int page) {
    if (page < 1 || page > _totalPage.value) return;
    _currentPage.value = page;
  }

  /// 更新总页数
  void updateTotalPage(int totalPage) {
    _totalPage.value = totalPage;
    // 如果当前页超出总页数，调整到最后一页
    if (_currentPage.value > totalPage && totalPage > 0) {
      _currentPage.value = totalPage;
    }
  }

  /// 设置页码按钮数量
  void setPageButtonCount(int count) {
    _pageButtonCount = count;
  }

  /// 设置页码按钮起始编号
  void setPageButtonNumberStart(int start) {
    _pageButtonNumberStart = start;
  }

  /// 更新页码按钮起始编号
  void _updatePageButtonNumberStart() {
    if (_totalPage.value <= _pageButtonCount) {
      _pageButtonNumberStart = 1;
    } else if (_currentPage.value >= _pageButtonNumberStart + _pageButtonCount) {
      _pageButtonNumberStart = _currentPage.value - _pageButtonCount + 1;
    } else if (_currentPage.value <= _pageButtonNumberStart) {
      _pageButtonNumberStart = _currentPage.value;
    }
  }

  /// 设置页码变更回调
  void setOnPageChanged(Function(int page)? callback) {
    _onPageChanged = callback;
  }

  /// 处理页码变更
  void handlePageChanged(int page) {
    if (page < 1 || page > _totalPage.value) return;

    updateCurrentPage(page);
    _updatePageButtonNumberStart();
    _onPageChanged?.call(page);
  }

  /// 跳转到上一页
  void goToPreviousPage() {
    if (_currentPage.value > 1) {
      handlePageChanged(_currentPage.value - 1);
    }
  }

  /// 跳转到下一页
  void goToNextPage() {
    if (_currentPage.value < _totalPage.value) {
      handlePageChanged(_currentPage.value + 1);
    }
  }

  /// 跳转到第一页
  void goToFirstPage() {
    if (_currentPage.value != 1) {
      handlePageChanged(1);
    }
  }

  /// 跳转到最后一页
  void goToLastPage() {
    if (_totalPage.value > 0 && _currentPage.value != _totalPage.value) {
      handlePageChanged(_totalPage.value);
    }
  }

  /// 检查是否有上一页
  bool get hasPreviousPage => _currentPage.value > 1;

  /// 检查是否有下一页
  bool get hasNextPage => _currentPage.value < _totalPage.value;

  /// 检查是否显示分页组件
  bool get shouldShowPagination => _totalPage.value >= 1;
}
