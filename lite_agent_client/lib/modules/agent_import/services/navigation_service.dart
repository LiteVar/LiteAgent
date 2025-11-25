import 'package:get/get.dart';
import 'package:lite_agent_client/modules/home/logic.dart';
import 'package:lite_agent_client/modules/agent_import/services/parsing_service.dart';
import 'package:lite_agent_client/modules/agent_import/services/import_service.dart';

/// 智能体导入步骤枚举
enum ImportStep {
  uploadFile, // 上传文件
  parsing, // 解析文件配置
  modelConfig, // 大模型配置
  toolConfig, // 工具配置
  knowledgeConfig, // 知识库配置
  agentConfig, // 智能体配置
  createConfig, // 创建配置
}

/// 导航服务
class NavigationService extends GetxService {
  late final HomePageLogic homeLogic;

  @override
  void onInit() {
    super.onInit();
    homeLogic = Get.find<HomePageLogic>();
  }

  /// 返回到 Agent 列表页面
  void backToAgentPage() {
    homeLogic.backToAgentPage();
  }

  /// 返回上一步
  void previousStep(Rx<ImportStep> currentStep) {
    switch (currentStep.value) {
      case ImportStep.uploadFile:
        // 第一步，不能返回
        break;
      case ImportStep.parsing:
        currentStep.value = ImportStep.uploadFile;
        break;
      case ImportStep.modelConfig:
        currentStep.value = ImportStep.parsing;
        break;
      case ImportStep.toolConfig:
        currentStep.value = ImportStep.modelConfig;
        break;
      case ImportStep.knowledgeConfig:
        currentStep.value = ImportStep.toolConfig;
        break;
      case ImportStep.agentConfig:
        currentStep.value = ImportStep.knowledgeConfig;
        break;
      case ImportStep.createConfig:
        // 从创建配置页面返回，清理导入记录
        final importService = Get.find<ImportService>();
        importService.reset();
        currentStep.value = ImportStep.agentConfig;
        break;
    }
  }

  /// 下一步
  Future<void> nextStep(Rx<ImportStep> currentStep) async {
    switch (currentStep.value) {
      case ImportStep.uploadFile:
        currentStep.value = ImportStep.parsing;
        break;
      case ImportStep.parsing:
        // 检查是否有错误
        final parsingService = Get.find<ParsingService>();
        if (parsingService.modelParseError.value != null ||
            parsingService.toolParseError.value != null ||
            parsingService.knowledgeBaseParseError.value != null ||
            parsingService.agentParseError.value != null) {
          backToAgentPage(); // 退出到Agent页面
          return;
        }

        // 检查是否还在解析
        if (parsingService.isParsingModels.value ||
            parsingService.isParsingTools.value ||
            parsingService.isParsingKnowledgeBases.value ||
            parsingService.isParsingAgents.value) {
          return;
        }

        // 所有解析完成，进入大模型配置展示
        currentStep.value = ImportStep.modelConfig;
        break;
      case ImportStep.modelConfig:
        // 进入工具配置展示
        currentStep.value = ImportStep.toolConfig;
        break;
      case ImportStep.toolConfig:
        // 进入知识库配置展示
        currentStep.value = ImportStep.knowledgeConfig;
        break;
      case ImportStep.knowledgeConfig:
        // 进入智能体配置展示
        currentStep.value = ImportStep.agentConfig;
        break;
      case ImportStep.agentConfig:
        // 进入创建配置步骤
        currentStep.value = ImportStep.createConfig;
        break;
      case ImportStep.createConfig:
        // 导入完成或失败，关闭导入页面
        backToAgentPage();
        break;
    }
  }

  /// 是否可以进入下一步
  bool canGoNext(Rx<ImportStep> currentStep, RxString uploadedFileName) {
    switch (currentStep.value) {
      case ImportStep.uploadFile:
        return uploadedFileName.value.isNotEmpty;
      case ImportStep.parsing:
        final parsingService = Get.find<ParsingService>();
        // 出错时允许点击"退出"；无错时需解析完成才能下一步
        final hasError = parsingService.modelParseError.value != null ||
            parsingService.toolParseError.value != null ||
            parsingService.knowledgeBaseParseError.value != null ||
            parsingService.agentParseError.value != null;
        if (hasError) return true;
        return !parsingService.isParsingModels.value &&
            !parsingService.isParsingTools.value &&
            !parsingService.isParsingKnowledgeBases.value &&
            !parsingService.isParsingAgents.value &&
            parsingService.modelParseError.value == null &&
            parsingService.toolParseError.value == null &&
            parsingService.knowledgeBaseParseError.value == null &&
            parsingService.agentParseError.value == null;
      case ImportStep.modelConfig:
        return true;
      case ImportStep.toolConfig:
        return true;
      case ImportStep.knowledgeConfig:
        return true;
      case ImportStep.agentConfig:
        return true;
      case ImportStep.createConfig:
        // 创建配置步骤：导入进行中不可点击；其他情况下可点击（用于开始或结束）
        final importService = Get.find<ImportService>();
        return !importService.isImporting.value;
    }
  }

  /// 是否可以返回上一步
  bool canGoPrevious(Rx<ImportStep> currentStep) {
    switch (currentStep.value) {
      case ImportStep.uploadFile:
        return false;
      case ImportStep.parsing:
        return true;
      case ImportStep.modelConfig:
        return true;
      case ImportStep.toolConfig:
        return true;
      case ImportStep.knowledgeConfig:
        return true;
      case ImportStep.agentConfig:
        return true;
      case ImportStep.createConfig:
        final importService = Get.find<ImportService>();
        final beforeStart =
            !importService.isImporting.value && importService.importError.value == null && importService.importProgressMessages.isEmpty;
        final failed = importService.importError.value != null;
        return beforeStart || failed;
    }
  }

  /// 获取下一步按钮文本
  String getNextButtonText(Rx<ImportStep> currentStep) {
    // 检查解析步骤是否有错误
    if (currentStep.value == ImportStep.parsing) {
      final parsingService = Get.find<ParsingService>();
      if (parsingService.modelParseError.value != null ||
          parsingService.toolParseError.value != null ||
          parsingService.knowledgeBaseParseError.value != null ||
          parsingService.agentParseError.value != null) {
        return '退出';
      }
    }

    // 检查创建配置步骤是否有错误
    if (currentStep.value == ImportStep.createConfig) {
      final importService = Get.find<ImportService>();
      if (importService.isImporting.value) {
        return '进行中';
      }
      if (importService.importError.value != null) {
        return '退出';
      }
      if (importService.importProgressMessages.isEmpty) {
        return '开始';
      }
      return '完成';
    }

    return '下一步';
  }
}
