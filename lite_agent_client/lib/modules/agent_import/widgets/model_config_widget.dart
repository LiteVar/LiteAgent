import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/utils/extension/model_extension.dart';
import 'package:lite_agent_client/widgets/common_dropdown.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';

import '../logic.dart';

/// 大模型配置组件
class ModelConfigWidget extends StatelessWidget {
  const ModelConfigWidget({super.key});

  final textColor = const Color(0xFF333333);
  final urlColor = const Color(0xFF666666);
  final actionColor = const Color(0xFF2a82f5);

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<AgentImportLogic>();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('大模型列表', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500)),
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
      final totalCount = logic.parsingService.parsedModels.length + logic.parsingService.knowledgeBaseModels.length;
      if (totalCount == 0) {
        return const Center(child: Text('暂无模型配置', style: TextStyle(fontSize: 14, color: Color(0xFF999999))));
      }

      return Column(
        children: [
          if (_hasSimilarModels(logic)) ...[_buildSimilarDataNote(), const SizedBox(height: 16)],
          Expanded(child: _buildTable(logic)),
          const SizedBox(height: 16),
          if (!logic.parsingService.isModelPlainText.value) _buildWarningNote(),
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
            ..._buildModelRows(logic),
          ],
        ),
      ),
    );
  }

  /// 构建表头
  Widget _buildTableHeader() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      decoration: const BoxDecoration(
        color: Color(0xFFfafafa),
        border: Border(bottom: BorderSide(color: Color(0xFFd9d9d9))),
      ),
      child: const Row(
        children: [
          Expanded(flex: 2, child: Center(child: Text('模型名称', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('模型别名', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 1, child: Center(child: Text('模型类型', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('URL', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('API Key', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 2, child: Center(child: Text('平台状态', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
          Expanded(flex: 1, child: Center(child: Text('操作', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)))),
        ],
      ),
    );
  }

  /// 构建模型数据行
  List<Widget> _buildModelRows(AgentImportLogic logic) {
    final allModels = [
      ...logic.parsingService.parsedModels.values,
      ...logic.parsingService.knowledgeBaseModels.values,
    ];
    return allModels.map((model) => _buildModelRow(logic, model)).toList();
  }

  // 操作选项：新建、覆盖、跳过
  static const operateItems = [
    DropdownItem<int>(value: ImportOperate.operateNew, text: '新建'),
    DropdownItem<int>(value: ImportOperate.operateOverwrite, text: '覆盖'),
    DropdownItem<int>(value: ImportOperate.operateSkip, text: '跳过'),
  ];

  /// 构建模型行
  Widget _buildModelRow(AgentImportLogic logic, ModelDTO model) {
    const textColor = Color(0xFF333333);
    const urlColor = Color(0xFF666666);
    const actionColor = Color(0xFF2a82f5);

    // 检查是否有重名模型（similarId不为空表示有重名）
    final similarSelectWidget = model.similarId.isNotEmpty
        ? _buildOperateOptionRow(logic, model)
        : const Text('-', style: TextStyle(fontSize: 14, color: Color(0xFF999999)));

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: Color(0xFFd9d9d9)))),
      child: Row(
        children: [
          Expanded(flex: 2, child: _buildAttrText(model.name, textColor)),
          Expanded(flex: 2, child: _buildAttrText(model.alias, textColor)),
          Expanded(flex: 1, child: _buildAttrText(model.type, textColor)),
          Expanded(flex: 2, child: _buildAttrText(model.baseUrl, urlColor)),
          Expanded(flex: 2, child: _buildAttrText(model.apiKey, urlColor)),
          Expanded(flex: 2, child: Center(child: similarSelectWidget)),
          Expanded(
            flex: 1,
            child: Center(
              child: InkWell(
                onTap: () => _showEditModelDialog(logic, model),
                child: const Text('编辑', style: TextStyle(fontSize: 14, color: actionColor), overflow: TextOverflow.ellipsis),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Center _buildAttrText(String text, Color textColor) {
    return Center(
        child: Text(text.isNotEmpty ? text : '-', style: TextStyle(fontSize: 14, color: textColor), overflow: TextOverflow.ellipsis));
  }

  Row _buildOperateOptionRow(AgentImportLogic logic, ModelDTO model) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Text('已有该模型', style: TextStyle(fontSize: 14, color: Color(0xFF000000))),
        CommonDropdown<int>(
          selectedValue: model.operate,
          items: operateItems,
          onChanged: (value) {
            model.operate = value ?? 0;
            logic.parsingService.parsedModels.refresh();
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

  /// 检查是否有相似模型
  bool _hasSimilarModels(AgentImportLogic logic) {
    var parsedHasSimilar = logic.parsingService.parsedModels.values.any((model) => model.similarId.isNotEmpty);
    var kbHasSimilar = logic.parsingService.knowledgeBaseModels.values.any((model) => model.similarId.isNotEmpty);
    return parsedHasSimilar || kbHasSimilar;
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
            child: Text('平台上已存在相同的模型配置。当前默认导入方式为 创建新模型，您可切换至 覆盖现有配置。', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
          ),
        ],
      ),
    );
  }

  /// 构建警告提示
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
            child: Text('如果大模型中未包含API Key，大模型将无法使用。可点击编辑进行配置或者在智能体创建完成后，前往 设置 -> 模型管理，手动补充您的授权密钥。',
                style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
          ),
        ],
      ),
    );
  }

  /// 显示编辑模型对话框
  void _showEditModelDialog(AgentImportLogic logic, ModelDTO model) {
    Get.dialog(
      barrierDismissible: false,
      EditModelDialog(
        model: model.toModel(),
        isEdit: true,
        isKnowledgeModel: true,
        onConfirmCallback: (ModelFormData? updatedModelData, {bool isDelete = false}) async {
          if (updatedModelData != null) {
            await _updateModel(model, updatedModelData);
            logic.parsingService.parsedModels.refresh();
          }
        },
      ),
    );
  }

  /// 更新逻辑中的模型数据
  Future<void> _updateModel(ModelDTO model, ModelFormData updatedData) async {
    model.alias = updatedData.alias;
    model.name = updatedData.name;
    model.baseUrl = updatedData.baseUrl;
    model.apiKey = updatedData.apiKey;
    model.maxTokens = int.tryParse(updatedData.maxToken) ?? 4096;
    model.type = updatedData.modelType;
    model.autoAgent = updatedData.supportMultiAgent;
    model.toolInvoke = updatedData.supportToolCalling;
    model.deepThink = updatedData.supportDeepThinking;
  }
}
