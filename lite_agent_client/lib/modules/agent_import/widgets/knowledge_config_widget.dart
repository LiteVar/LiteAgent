import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/widgets/common_dropdown.dart';
import '../logic.dart';

/// 知识库配置组件
class KnowledgeConfigWidget extends StatelessWidget {
  const KnowledgeConfigWidget({super.key});

  final textColor = const Color(0xFF333333);
  final descColor = const Color(0xFF666666);
  final emptyColor = const Color(0xFF999999);

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<AgentImportLogic>();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('知识库列表', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500)),
        const SizedBox(height: 16),
        Expanded(
          child: _buildContent(logic),
        ),
      ],
    );
  }

  /// 构建内容区域
  Widget _buildContent(AgentImportLogic logic) {
    return Obx(() {
      if (logic.parsingService.parsedKnowledgeBases.isEmpty) {
        return const Center(child: Text('暂无知识库配置', style: TextStyle(fontSize: 14, color: Color(0xFF999999))));
      }

      return Column(
        children: [
          if (_hasSimilarKnowledgeBases(logic)) ...[_buildSimilarDataNote(), const SizedBox(height: 16)],
          Expanded(child: _buildTable(logic)),
          const SizedBox(height: 16),
          if (!logic.parsingService.isKnowledgeBasePlainText.value) _buildEmbeddingWarningNote(),
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
            ..._buildKnowledgeBaseRows(logic),
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
          Expanded(flex: 2, child: Center(child: Text('知识库名称', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('描述', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 1, child: Center(child: Text('文件数量', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('向量化模型', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('平台状态', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
        ],
      ),
    );
  }

  /// 构建知识库数据行
  List<Widget> _buildKnowledgeBaseRows(AgentImportLogic logic) {
    return logic.parsingService.parsedKnowledgeBases.entries.map((entry) => _buildKnowledgeBaseRow(logic, entry.value)).toList();
  }

  // 操作选项：新建、覆盖、跳过
  static const operateItems = [
    DropdownItem<int>(value: ImportOperate.operateNew, text: '新建'),
    DropdownItem<int>(value: ImportOperate.operateOverwrite, text: '覆盖'),
    DropdownItem<int>(value: ImportOperate.operateSkip, text: '跳过'),
  ];

  /// 构建知识库行
  Widget _buildKnowledgeBaseRow(AgentImportLogic logic, LibraryDto kb) {
    // 检查是否有重名知识库（similarId不为空表示有重名）
    final similarSelectWidget = kb.similarId != null && kb.similarId!.isNotEmpty
        ? _buildOperateOptionRow(logic, kb)
        : const Text('-', style: TextStyle(fontSize: 14, color: Color(0xFF999999)));

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: Color(0xFFd9d9d9)))),
      child: Row(
        children: [
          Expanded(flex: 2, child: _buildAttrText(kb.name, textColor)),
          Expanded(flex: 2, child: _buildAttrText(kb.description, descColor)),
          Expanded(flex: 1, child: _buildAttrText('${kb.docCount}个', textColor)),
          Expanded(
            flex: 2,
            child: Obx(() {
              // 获取embedding模型名称
              final embeddingModel = logic.parsingService.knowledgeBaseModels[kb.embeddingModelId];
              return _buildAttrText(embeddingModel?.alias ?? '-', textColor);
            }),
          ),
          Expanded(flex: 2, child: Center(child: similarSelectWidget)),
        ],
      ),
    );
  }

  Center _buildAttrText(String text, Color textColor) {
    return Center(
        child: Text(text.isNotEmpty ? text : '-', style: TextStyle(fontSize: 14, color: textColor), overflow: TextOverflow.ellipsis));
  }

  Row _buildOperateOptionRow(AgentImportLogic logic, LibraryDto kb) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Text('已有该知识库', style: TextStyle(fontSize: 14, color: Color(0xFF000000))),
        CommonDropdown<int>(
          selectedValue: kb.operate,
          items: operateItems,
          onChanged: (value) {
            kb.operate = value ?? 0;
            logic.parsingService.parsedKnowledgeBases.refresh();
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

  /// 检查是否有相似知识库
  bool _hasSimilarKnowledgeBases(AgentImportLogic logic) {
    return logic.parsingService.parsedKnowledgeBases.values.any((kb) => kb.similarId != null && kb.similarId!.isNotEmpty);
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
            child: Text('平台上已存在相同的知识库配置。当前默认导入方式为 创建新知识库，您可切换至 覆盖现有配置。', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
          ),
        ],
      ),
    );
  }

  /// 构建底部提示
  Widget _buildEmbeddingWarningNote() {
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
              '如果在大模型配置中，embadding模型未配置，则知识库创建后，文件将无法进行向量化，您可以在智能体创建后，在知识库管理-embadding设置中选择一个模型进行向量化设置',
              style: TextStyle(fontSize: 14, color: Color(0xFF666666)),
            ),
          ),
        ],
      ),
    );
  }
}
