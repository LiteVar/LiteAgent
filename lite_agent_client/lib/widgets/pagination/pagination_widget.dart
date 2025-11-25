import 'dart:math';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/pagination/pagination_controller.dart';

import '../common_widget.dart';

/// 通用分页组件
class PaginationWidget extends StatelessWidget {
  /// 分页控制器
  final PaginationController controller;

  /// 容器边距
  final EdgeInsetsGeometry? margin;

  const PaginationWidget({Key? key, required this.controller, this.margin}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (!controller.shouldShowPagination) return Container();

    return Container(
      margin: margin ?? const EdgeInsets.fromLTRB(20, 20, 60, 20),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          _buildNavigationButton(onTap: controller.goToPreviousPage, iconName: "icon_button_left.png", enabled: controller.hasPreviousPage),
          // 页码按钮 - 使用 Obx 观察 totalPage 和 pageButtonNumberStart 变化
          Obx(() => Row(
                children: List.generate(
                  min(controller.pageButtonCount, controller.totalPage.value),
                  (index) => _buildPageButton(controller.pageButtonNumberStart + index),
                ),
              )),
          _buildNavigationButton(onTap: controller.goToNextPage, iconName: "icon_button_right.png", enabled: controller.hasNextPage),
        ],
      ),
    );
  }

  /// 构建导航按钮（上一页/下一页）
  Widget _buildNavigationButton({required VoidCallback onTap, required String iconName, bool enabled = true}) {
    return Row(
      children: [
        const SizedBox(width: 10),
        InkWell(
          onTap: enabled ? onTap : null,
          child: buildAssetImage(iconName, 30, enabled ? const Color(0xff666666) : const Color(0xffcccccc)),
        ),
      ],
    );
  }

  /// 构建页码按钮
  Widget _buildPageButton(int page) {
    return Obx(
      () {
        var isSelected = controller.currentPage.value == page;
        var selectedBoxDecoration = BoxDecoration(color: const Color(0xff337fe3), borderRadius: BorderRadius.circular(4));
        var unselectedBoxDecoration =
            BoxDecoration(border: Border.all(color: const Color(0xfff5f5f5)), borderRadius: BorderRadius.circular(4));
        return Row(
          children: [
            const SizedBox(width: 10),
            InkWell(
                onTap: () => controller.handlePageChanged(page),
                child: Container(
                    width: 30,
                    height: 30,
                    decoration: isSelected ? selectedBoxDecoration : unselectedBoxDecoration,
                    child: Center(
                        child: Text("$page", style: TextStyle(fontSize: 16, color: isSelected ? Colors.white : const Color(0xff666666))))))
          ],
        );
      },
    );
  }
}
