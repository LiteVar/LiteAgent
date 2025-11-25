import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/widgets/common_dropdown.dart';
import '../logic.dart';

/// 工具配置组件
class ToolConfigWidget extends StatelessWidget {
  const ToolConfigWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<AgentImportLogic>();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('工具列表', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500)),
        const SizedBox(height: 16),
        Expanded(child: _buildContent(logic))
      ],
    );
  }

  /// 构建内容区域
  Widget _buildContent(AgentImportLogic logic) {
    return Obx(() {
      if (logic.parsingService.parsedTools.isEmpty) {
        return const Center(child: Text('暂无工具配置', style: TextStyle(fontSize: 14, color: Color(0xFF999999))));
      }

      return Column(
        children: [
          if (_hasSimilarTools(logic)) ...[_buildSimilarDataNote(), const SizedBox(height: 16)],
          Expanded(child: _buildTable(logic)),
          const SizedBox(height: 16),
          if (!logic.parsingService.isToolPlainText.value) _buildWarningNote(),
        ],
      );
    });
  }

  /// 构建表格
  Widget _buildTable(AgentImportLogic logic) {
    return SingleChildScrollView(
      child: Container(
        decoration: BoxDecoration(border: Border.all(color: const Color(0xFFd9d9d9)), borderRadius: BorderRadius.circular(4)),
        child: Column(
          children: [
            _buildTableHeader(),
            ..._buildToolRows(logic),
          ],
        ),
      ),
    );
  }

  /// 构建表头
  Widget _buildTableHeader() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: const BoxDecoration(
        color: Color(0xFFfafafa),
        border: Border(bottom: BorderSide(color: Color(0xFFd9d9d9))),
      ),
      child: const Row(
        children: [
          Expanded(flex: 2, child: Center(child: Text('工具名称', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('平台状态', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
        ],
      ),
    );
  }

  /// 构建工具数据行
  List<Widget> _buildToolRows(AgentImportLogic logic) {
    return logic.parsingService.parsedTools.values.map((tool) => _buildToolRow(logic, tool)).toList();
  }

  // 操作选项：新建、覆盖、跳过
  static const operateItems = [
    DropdownItem<int>(value: ImportOperate.operateNew, text: '新建'),
    DropdownItem<int>(value: ImportOperate.operateOverwrite, text: '覆盖'),
    DropdownItem<int>(value: ImportOperate.operateSkip, text: '跳过'),
  ];

  /// 构建工具行
  Widget _buildToolRow(AgentImportLogic logic, tool) {
    const textColor = Color(0xFF333333);

    // 检查是否有重名工具（similarId不为空表示有重名）
    final similarSelectWidget = tool.similarId.isNotEmpty
        ? _buildOperateOptionRow(logic, tool)
        : const Text('-', style: TextStyle(fontSize: 14, color: Color(0xFF999999)));

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: Color(0xFFd9d9d9)))),
      child: Row(
        children: [
          Expanded(flex: 2, child: _buildAttrText(tool.name, textColor)),
          Expanded(flex: 2, child: Center(child: similarSelectWidget)),
        ],
      ),
    );
  }

  Center _buildAttrText(String text, Color textColor) {
    return Center(
        child: Text(text.isNotEmpty ? text : '-', style: TextStyle(fontSize: 14, color: textColor), overflow: TextOverflow.ellipsis));
  }

  Row _buildOperateOptionRow(AgentImportLogic logic, tool) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Text('已有该工具', style: TextStyle(fontSize: 14, color: Color(0xFF000000))),
        CommonDropdown<int>(
          selectedValue: tool.operate,
          items: operateItems,
          onChanged: (value) {
            tool.operate = value ?? 0;
            logic.parsingService.parsedTools.refresh();
          },
          placeholder: '选择操作',
          width: 50,
          height: 28,
          fontSize: 14,
          textColor: const Color(0xFF2a82f5),
          iconColor: const Color(0xFF2a82f5),
          backgroundColor: Colors.transparent,
          margin: EdgeInsets.zero,
          padding: const EdgeInsets.symmetric(horizontal: 4),
          dropdownWidth: 60,
        ),
      ],
    );
  }

  /// 检查是否有相似工具
  bool _hasSimilarTools(AgentImportLogic logic) {
    return logic.parsingService.parsedTools.values.any((tool) => tool.similarId?.isNotEmpty ?? false);
  }

  /// 构建相似数据提示
  Widget _buildSimilarDataNote() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBE6),
        border: Border.all(color: const Color(0xFFFFE58F)),
        borderRadius: BorderRadius.circular(4),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('[注意] ', style: TextStyle(fontSize: 14, color: Color(0xFFfa8c16), fontWeight: FontWeight.w500)),
          Expanded(
            child: Text('平台上已存在相同的工具配置。当前默认导入方式为 创建新工具，您可切换至 覆盖现有配置。', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
          ),
        ],
      ),
    );
  }

  /// 构建底部警告提示
  Widget _buildWarningNote() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBE6),
        border: Border.all(color: const Color(0xFFFFE58F)),
        borderRadius: BorderRadius.circular(4),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('[注意] ', style: TextStyle(fontSize: 14, color: Color(0xFFfa8c16), fontWeight: FontWeight.w500)),
          Expanded(
            child: Text(
              '如果外部工具中未包含API Key，可能无法启用，在智能体创建完成后，前往 设置 -> 工具管理，手动补充您的授权密钥。',
              style: TextStyle(fontSize: 14, color: Color(0xFF666666)),
            ),
          ),
        ],
      ),
    );
  }
}
