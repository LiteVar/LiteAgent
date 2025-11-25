import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/model.dart';

/// 模型状态管理器
/// 负责管理LLM、TTS、ASR模型数据和UI状态
class ModelStateManager {
  // 模型列表数据
  final RxList<ModelData> llmModelList = <ModelData>[].obs;
  final RxList<ModelData> ttsModelList = <ModelData>[].obs;
  final RxList<ModelData> asrModelList = <ModelData>[].obs;
  final RxList<ModelData> autoAgentModelList = <ModelData>[].obs;
  
  // 当前选中的模型
  final RxString currentLLMModelId = "".obs;
  final RxString currentTTSModelId = "".obs;
  final RxString currentASRModelId = "".obs;
  ModelData? currentModel;
  
  // 功能开关
  final RxBool enableTextToSpeech = false.obs;
  final RxBool enableSpeechToText = false.obs;
  
  // UI状态
  final RxBool isAudioExpanded = false.obs;
  final RxBool isModelExpanded = false.obs;
  final RxBool showMoreModel = false.obs;
  final RxString modelHoverItemId = "".obs;

  void updateLLMModelList(List<ModelData> models) {
    llmModelList.assignAll(models);
  }

  void updateTTSModelList(List<ModelData> models) {
    ttsModelList.assignAll(models);
  }

  void updateASRModelList(List<ModelData> models) {
    asrModelList.assignAll(models);
  }

  void updateAutoAgentModelList(List<ModelData> models) {
    autoAgentModelList.assignAll(models);
  }

  void selectModel(ModelData? model) {
    currentModel = model;
  }

  void selectLLMModel(String modelId) {
    currentLLMModelId.value = modelId;
  }

  void selectTTSModel(String modelId) {
    currentTTSModelId.value = modelId;
  }

  void selectASRModel(String modelId) {
    currentASRModelId.value = modelId;
  }

  void toggleTextToSpeech(bool enabled) {
    enableTextToSpeech.value = enabled;
    if (!enabled) {
      currentTTSModelId.value = "";
    }
  }

  void toggleSpeechToText(bool enabled) {
    enableSpeechToText.value = enabled;
    if (!enabled) {
      currentASRModelId.value = "";
    }
  }

  void toggleAudioExpanded(bool expanded) {
    isAudioExpanded.value = expanded;
  }

  void toggleModelExpanded(bool expanded) {
    isModelExpanded.value = expanded;
  }

  void toggleShowMoreModel(bool showMore) {
    showMoreModel.value = showMore;
  }

  void hoverModelItem(String id) {
    modelHoverItemId.value = id;
  }

  // 获取当前LLM模型
  ModelData? get currentLLMModel {
    if (currentLLMModelId.value.isEmpty) return null;
    return llmModelList.firstWhereOrNull((model) => model.id == currentLLMModelId.value);
  }

  // 获取当前TTS模型
  ModelData? get currentTTSModel {
    if (currentTTSModelId.value.isEmpty) return null;
    return ttsModelList.firstWhereOrNull((model) => model.id == currentTTSModelId.value);
  }

  // 获取当前ASR模型
  ModelData? get currentASRModel {
    if (currentASRModelId.value.isEmpty) return null;
    return asrModelList.firstWhereOrNull((model) => model.id == currentASRModelId.value);
  }

  // 检查是否有LLM模型
  bool get hasLLMModels => llmModelList.isNotEmpty;

  // 检查是否有TTS模型
  bool get hasTTSModels => ttsModelList.isNotEmpty;

  // 检查是否有ASR模型
  bool get hasASRModels => asrModelList.isNotEmpty;

  // 获取当前模型列表
  List<ModelData> get currentModelList => llmModelList;

  // 获取模型数量
  int get modelCount => llmModelList.length;

  // 检查是否有模型
  bool get hasModels => llmModelList.isNotEmpty;
}
