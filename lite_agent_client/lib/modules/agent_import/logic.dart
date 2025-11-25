import 'package:get/get.dart';
import 'package:lite_agent_client/modules/agent_import/services/file_service.dart';
import 'package:lite_agent_client/modules/agent_import/services/parsing_service.dart';
import 'package:lite_agent_client/modules/agent_import/services/import_service.dart';
import 'package:lite_agent_client/modules/agent_import/services/navigation_service.dart';

import '../../widgets/dialog/dialog_common_confirm.dart';

/// 简化后的Agent导入逻辑控制器
class AgentImportLogic extends GetxController {
  // 服务依赖
  late final FileService fileService;
  late final ParsingService parsingService;
  late final ImportService importService;
  late final NavigationService navigationService;

  // 当前步骤
  final currentStep = Rx<ImportStep>(ImportStep.uploadFile);

  @override
  void onInit() {
    super.onInit();

    // 初始化服务
    fileService = Get.put(FileService());
    parsingService = Get.put(ParsingService());
    importService = Get.put(ImportService());
    navigationService = Get.put(NavigationService());

    resetForm();
  }

  @override
  void onClose() {
    // 退出前先重置状态，确保下次进入时是干净的
    resetForm();
    // 清理临时文件
    fileService.cleanupTempDir();
    // 清理服务
    Get.delete<FileService>();
    Get.delete<ParsingService>();
    Get.delete<ImportService>();
    Get.delete<NavigationService>();
    super.onClose();
  }

  /// 重置表单
  void resetForm() {
    currentStep.value = ImportStep.uploadFile;
    fileService.reset();
    parsingService.reset();
    importService.reset();
  }

  /// 返回到 Agent 列表页面
  void backToAgentPage() {
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
            title: '确认退出',
            content: '当前正在导入智能体，退出页面将丢失当前进度，是否确认退出？',
            confirmString: '确认',
            onConfirmCallback: () async {
              resetForm();
              navigationService.backToAgentPage();
            }));
  }

  /// 选择文件
  Future<void> selectFile() async {
    await fileService.selectFile();
    // 文件选择成功后，重置解析状态
    if (fileService.selectedFile.value != null) {
      parsingService.reset();
    }
  }

  /// 处理拖拽文件
  Future<void> handleDragFile(List<dynamic> files) async {
    await fileService.handleDragFile(files.cast());
    // 文件选择成功后，重置解析状态
    if (fileService.selectedFile.value != null) {
      parsingService.reset();
    }
    // 拖拽文件后不自动跳转，等待用户点击下一步
  }

  /// 更换文件
  Future<void> changeFile() async {
    await fileService.changeFile();
    // 文件选择成功后，重置解析状态
    if (fileService.selectedFile.value != null) {
      parsingService.reset();
    }
  }

  /// 上传并进入下一步（进入解析步骤）
  Future<void> uploadAndNext() async {
    if (fileService.selectedFile.value == null) {
      return;
    }
    currentStep.value = ImportStep.parsing;
  }

  /// 开始解析所有配置
  Future<void> startParsing() async {
    await parsingService.startParsing();
  }

  /// 返回上一步
  void previousStep() {
    navigationService.previousStep(currentStep);
  }

  /// 下一步
  Future<void> nextStep() async {
    await navigationService.nextStep(currentStep);
  }

  /// 是否可以进入下一步
  bool canGoNext() {
    return navigationService.canGoNext(currentStep, fileService.uploadedFileName);
  }

  /// 是否可以返回上一步
  bool canGoPrevious() {
    return navigationService.canGoPrevious(currentStep);
  }

  /// 获取下一步按钮文本
  String getNextButtonText() {
    return navigationService.getNextButtonText(currentStep);
  }

  /// 执行导入
  Future<void> executeImport() async {
    await importService.executeImport();
  }
}
