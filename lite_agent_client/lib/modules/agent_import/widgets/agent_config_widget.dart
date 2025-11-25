import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/utils/agent/agent_converter.dart';
import 'package:lite_agent_client/widgets/common_dropdown.dart';
import '../logic.dart';
import 'package:lite_agent_client/models/dto/agent.dart';

/// 智能体配置组件
class AgentConfigWidget extends StatelessWidget {
  const AgentConfigWidget({super.key});

  // 操作选项：新建、覆盖、跳过
  static const operateItems = [
    DropdownItem<int>(value: ImportOperate.operateNew, text: '新建'),
    DropdownItem<int>(value: ImportOperate.operateOverwrite, text: '覆盖'),
    DropdownItem<int>(value: ImportOperate.operateSkip, text: '跳过'),
  ];

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<AgentImportLogic>();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('智能体列表', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500)),
        const SizedBox(height: 16),
        Expanded(
          child: Obx(() {
            final totalCount = (logic.parsingService.rootAgent.value != null ? 1 : 0) + logic.parsingService.parsedAgents.length;
            if (totalCount == 0) {
              return const Center(
                child: Text('暂无智能体配置', style: TextStyle(fontSize: 14, color: Color(0xFF999999))),
              );
            }

            return SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 相似数据提示
                  if (_hasSimilarAgents(logic)) ...[_buildSimilarDataNote(), const SizedBox(height: 16)],
                  // 主智能体
                  if (logic.parsingService.rootAgent.value != null) ...[
                    const Text('主智能体', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
                    const SizedBox(height: 12),
                    _buildAgentCard(logic.parsingService.rootAgent.value!, logic, isRoot: true),
                    const SizedBox(height: 24),
                  ],
                  // 子智能体
                  if (logic.parsingService.parsedAgents.isNotEmpty) ...[
                    const Text('子智能体', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
                    const SizedBox(height: 12),
                    ...logic.parsingService.parsedAgents.entries.map((entry) {
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: _buildAgentCard(entry.value, logic, isRoot: false),
                      );
                    }),
                  ],
                ],
              ),
            );
          }),
        )
      ],
    );
  }

  /// 构建智能体卡片
  Widget _buildAgentCard(AgentDTO agent, AgentImportLogic logic, {required bool isRoot}) {
    // 获取模型名称
    String getModelName(String modelId) {
      if (modelId.isEmpty) return '无';
      // 先从普通模型中查找
      final model = logic.parsingService.parsedModels[modelId];
      if (model != null) {
        return model.alias.isNotEmpty ? model.alias : modelId;
      }
      // 如果没找到，直接返回modelId
      return modelId;
    }

    final toolFunctionCount = logic.parsingService.functionRefs[agent.id]?.length ?? 0;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: const Color(0xFFEEF3FF), borderRadius: BorderRadius.circular(4)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(agent.name, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: Color(0xFF333333))),
              const SizedBox(width: 8),
              Text('(${AgentConverter.typeToString(agent.type)})',
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.normal, color: Color(0xFF666666))),
            ],
          ),
          const SizedBox(height: 12),
          _buildInfoRow('描述：', agent.description.isEmpty ? '无' : agent.description),
          const SizedBox(height: 8),
          _buildInfoRow('提示词：', agent.prompt.isEmpty ? '无' : agent.prompt),
          const SizedBox(height: 8),
          _buildInfoRow('模型：', getModelName(agent.llmModelId)),
          if (isRoot) ...[
            const SizedBox(height: 8),
            _buildInfoRow('子智能体数量：', '${logic.parsingService.parsedAgents.length}个'),
            const SizedBox(height: 8),
            _buildInfoRow('工具数量：', '$toolFunctionCount个'),
            const SizedBox(height: 8),
            _buildInfoRow('知识库数量：', '${agent.datasetIds?.length ?? 0}个'),
          ],
          if (agent.similarId != null && agent.similarId!.isNotEmpty) ...[
            const SizedBox(height: 12),
            _buildOperateOptionRow(logic, agent),
          ],
        ],
      ),
    );
  }

  /// 构建操作选项行
  Widget _buildOperateOptionRow(AgentImportLogic logic, AgentDTO agent) {
    return Row(
      children: [
        const Text('已有该智能体', style: TextStyle(fontSize: 12, color: Color(0xFF666666))),
        const SizedBox(width: 8),
        CommonDropdown<int>(
          selectedValue: agent.operate,
          items: operateItems,
          onChanged: (value) {
            agent.operate = value ?? 0;
            // 根据是主智能体还是子智能体来刷新
            if (logic.parsingService.rootAgent.value?.id == agent.id) {
              logic.parsingService.rootAgent.refresh();
            } else {
              logic.parsingService.parsedAgents.refresh();
            }
          },
          placeholder: '选择操作',
          width: 60,
          height: 24,
          fontSize: 12,
          textColor: const Color(0xFF1890FF),
          iconColor: const Color(0xFF1890FF),
          backgroundColor: Colors.transparent,
          margin: EdgeInsets.zero,
          padding: const EdgeInsets.symmetric(horizontal: 4),
          dropdownWidth: 80,
          iconSize: 14,
        ),
      ],
    );
  }

  /// 构建信息行
  Widget _buildInfoRow(String label, String value) {
    final normalizedValue = value.replaceAll(RegExp(r'\s+'), ' ').trim();

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: Color(0xFF666666))),
        Expanded(
            child: Text(normalizedValue,
                style: const TextStyle(fontSize: 12, color: Color(0xFF333333)), maxLines: 2, overflow: TextOverflow.ellipsis)),
      ],
    );
  }

  /// 检查是否有相似智能体
  bool _hasSimilarAgents(AgentImportLogic logic) {
    final rootHasSimilar =
        logic.parsingService.rootAgent.value?.similarId != null && logic.parsingService.rootAgent.value!.similarId!.isNotEmpty;
    final subHasSimilar = logic.parsingService.parsedAgents.values.any((agent) => agent.similarId != null && agent.similarId!.isNotEmpty);
    return rootHasSimilar || subHasSimilar;
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
            child: Text('平台上已存在相同的智能体配置。当前默认导入方式为 创建新智能体，您可切换至 覆盖现有配置。', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
          ),
        ],
      ),
    );
  }
}
