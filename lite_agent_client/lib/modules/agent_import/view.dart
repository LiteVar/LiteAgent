import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'package:lite_agent_client/modules/agent_import/logic.dart';
import 'package:lite_agent_client/modules/agent_import/services/navigation_service.dart';
import 'widgets/upload_file_widget.dart';
import 'widgets/parsing_widget.dart';
import 'widgets/model_config_widget.dart';
import 'widgets/tool_config_widget.dart';
import 'widgets/knowledge_config_widget.dart';
import 'widgets/agent_config_widget.dart';
import 'widgets/create_config_widget.dart';

/// Agent 导入页面
class AgentImportPage extends StatelessWidget {
  const AgentImportPage({super.key});

  @override
  Widget build(BuildContext context) {
    // 先删除旧实例，确保每次进入都是干净的状态
    Get.delete<AgentImportLogic>();
    final logic = Get.put(AgentImportLogic());

    return Scaffold(
      body: Container(
        color: Colors.white,
        child: Column(
          children: [
            // 顶部标题栏
            Container(padding: const EdgeInsets.all(20), child: _buildTopBar(logic)),
            horizontalLine(),
            Expanded(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 左侧导航
                  SizedBox(width: 200, child: _buildLeftNavigation(logic)),
                  verticalLine(),
                  // 右侧内容区域
                  Expanded(child: _buildRightContent(logic)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Container _buildRightContent(AgentImportLogic logic) {
    return Container(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          Expanded(
            child: Obx(() {
              switch (logic.currentStep.value) {
                case ImportStep.uploadFile:
                  return const UploadFileWidget();
                case ImportStep.parsing:
                  return const ParsingWidget();
                case ImportStep.modelConfig:
                  return const ModelConfigWidget();
                case ImportStep.toolConfig:
                  return const ToolConfigWidget();
                case ImportStep.knowledgeConfig:
                  return const KnowledgeConfigWidget();
                case ImportStep.agentConfig:
                  return const AgentConfigWidget();
                case ImportStep.createConfig:
                  return const CreateConfigWidget();
              }
            }),
          ),
          const SizedBox(height: 20),
          _buildBottomButtons(logic),
        ],
      ),
    );
  }

  /// 构建顶部标题栏
  Widget _buildTopBar(AgentImportLogic logic) {
    return Row(
      children: [
        InkWell(
          onTap: () => logic.backToAgentPage(),
          child: Container(margin: const EdgeInsets.only(right: 15), child: buildAssetImage("icon_back.png", 20, Colors.black)),
        ),
        const SizedBox(width: 10),
        const Text('导入智能体', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w500, color: Color(0xFF333333))),
      ],
    );
  }

  /// 构建左侧导航
  Widget _buildLeftNavigation(AgentImportLogic logic) {
    return Obx(() {
      return Column(
        children: [
          _buildNavItem(logic, '导入文件', ImportStep.uploadFile),
          _buildNavItem(logic, '解析文件配置', ImportStep.parsing),
          _buildNavItem(logic, '大模型配置', ImportStep.modelConfig),
          _buildNavItem(logic, '工具配置', ImportStep.toolConfig),
          _buildNavItem(logic, '知识库配置', ImportStep.knowledgeConfig),
          _buildNavItem(logic, '智能体配置', ImportStep.agentConfig),
          _buildNavItem(logic, '创建配置', ImportStep.createConfig),
        ],
      );
    });
  }

  /// 构建导航项
  Widget _buildNavItem(AgentImportLogic logic, String title, ImportStep step) {
    final isActive = logic.currentStep.value == step;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      decoration: BoxDecoration(color: isActive ? const Color(0xFFf0f0f0) : Colors.transparent, borderRadius: BorderRadius.circular(4)),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 14,
          color: isActive ? const Color(0xFF333333) : const Color(0xFF666666),
          fontWeight: isActive ? FontWeight.w500 : FontWeight.normal,
        ),
      ),
    );
  }

  /// 构建底部按钮
  Widget _buildBottomButtons(AgentImportLogic logic) {
    return Obx(() {
      final canGoPrevious = logic.canGoPrevious();
      final canGoNext = logic.canGoNext();
      final nextButtonText = logic.getNextButtonText();
      bool showPrevButton = canGoPrevious;
      bool prevEnabled = canGoPrevious;
      if (logic.currentStep.value == ImportStep.createConfig) {
        final importing = logic.importService.isImporting.value;
        final hasError = logic.importService.importError.value != null;
        final hasMessages = logic.importService.importProgressMessages.isNotEmpty;
        final beforeStart = !importing && !hasError && !hasMessages;
        final failed = hasError;
        showPrevButton = beforeStart || importing || failed;
        prevEnabled = beforeStart || failed; // 导入中不可点击
      }

      return Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          // 上一步按钮
          if (showPrevButton)
            ElevatedButton(
              onPressed: prevEnabled ? () => logic.previousStep() : null,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFf5f5f5),
                foregroundColor: const Color(0xFF666666),
                elevation: 0,
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(6),
                  side: const BorderSide(color: Color(0xFFd9d9d9)),
                ),
              ),
              child: const Text('上一步'),
            )
          else
            const SizedBox.shrink(),
          const SizedBox(width: 20),
          // 下一步/完成按钮
          ElevatedButton(
            onPressed: canGoNext
                ? () {
                    if (logic.currentStep.value == ImportStep.createConfig) {
                      // 创建配置步骤：如果尚未开始且不在导入中，则开始导入；否则执行原有下一步逻辑
                      final hasMessages = logic.importService.importProgressMessages.isNotEmpty;
                      final hasError = logic.importService.importError.value != null;
                      final importing = logic.importService.isImporting.value;
                      if (!hasMessages && !hasError && !importing) {
                        logic.executeImport();
                      } else {
                        logic.nextStep();
                      }
                    } else {
                      logic.nextStep();
                    }
                  }
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: canGoNext ? const Color(0xFF2a82f5) : const Color(0xFFf5f5f5),
              foregroundColor: canGoNext ? Colors.white : const Color(0xFF999999),
              elevation: 0,
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(6),
                side: BorderSide(color: canGoNext ? const Color(0xFF2a82f5) : const Color(0xFFd9d9d9)),
              ),
            ),
            child: Text(nextButtonText),
          ),
        ],
      );
    });
  }
}
