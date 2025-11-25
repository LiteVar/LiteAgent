import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_agent_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_common_confirm.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_markdown.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_select_child_agent.dart';

import '../../../utils/agent/agent_validator.dart';
import '../../../utils/alarm_util.dart';
import '../../../widgets/dialog/dialog_export_confirm.dart';
import '../logic.dart';

/// Dialog管理器
/// 统一管理AdjustmentPage中的所有dialog
class DialogManager {
  final AdjustmentLogic logic;

  DialogManager(this.logic);

  /// 显示返回确认对话框
  void showGoBackConfirmDialog() {
    Get.dialog(
      CommonConfirmDialog(
        title: "系统信息",
        content: "有内容未保存，确认离开？",
        confirmString: "",
        onConfirmCallback: () async => Get.back(),
      ),
    );
  }

  /// 显示删除Agent确认对话框
  void showRemoveAgentDialog(String id) {
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
          title: "删除确认",
          content: "即将删除Agent的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () async {
            await logic.confirmRemoveAgent(id);
          },
        ));
  }

  /// 显示编辑Agent对话框
  void showEditAgentDialog() {
    var targetAgent = logic.agent.value;
    if (targetAgent != null) {
      Get.dialog(
          barrierDismissible: false,
          EditAgentDialog(
              agent: targetAgent,
              isEdit: targetAgent.id.isNotEmpty,
              onConfirmCallback: (name, iconPath, description) async {
                await logic.handleEditAgent(name, iconPath, description);
              }));
    }
  }

  /// 显示创建模型对话框
  void showCreateModelDialog() {
    Get.dialog(
        barrierDismissible: false,
        EditModelDialog(
            model: null,
            isEdit: false,
            onConfirmCallback: (ModelFormData? modelData, {bool isDelete = false}) async {
              if (modelData != null) {
                await logic.handleCreateModel(modelData);
              }
            }));
  }

  /// 显示提示词预览对话框
  void showPromptPreviewDialog() {
    String prompt = logic.promptController.text;
    Get.dialog(
      barrierDismissible: false,
      MarkDownTextDialog(titleText: "提示词预览", contentText: prompt),
    );
  }

  /// 显示子Agent选择对话框
  void showChildAgentSelectDialog() {
    if (logic.agentStateManager.agentType.value == AgentValidator.DTO_TYPE_REFLECTION) {
      AlarmUtil.showAlertToast("反思类型不能添加子Agent");
      return;
    }
    Get.dialog(
      barrierDismissible: false,
      SelectChildAgentDialog(
        selectAgents: logic.agentStateManager.childAgentList,
        currentAgentId: logic.agent.value?.id ?? "",
        onSelectChanged: () => logic.isAgentChangeWithoutSave = true,
      ),
    );
  }

  /// 显示导出确认对话框
  void showExportConfirmDialog() {
    Get.dialog(
        barrierDismissible: false,
        ExportConfirmDialog(
          exportType: "智能体",
          onConfirmCallback: (exportPlaintext) => logic.exportCurrentAgent(exportPlaintext),
        ));
  }
}
