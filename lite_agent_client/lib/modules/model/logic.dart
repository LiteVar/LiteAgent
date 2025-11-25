import 'dart:async';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/utils/snowflake_util.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_detail.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/local/model.dart';
import '../../utils/model/model_import.dart';
import '../../widgets/dialog/dialog_common_confirm.dart';

class ModelLogic extends GetxController with WindowListener {
  late StreamSubscription _subscription;

  var modelList = <ModelData>[].obs;

  @override
  void onInit() {
    super.onInit();
    initWindow();
    loadData();
    initEventBus();
  }

  void initWindow() async {
    windowManager.addListener(this);
    await windowManager.setPreventClose(true);
  }

  void loadData() async {
    modelList.assignAll((await modelRepository.getModelListFromBox()));
    modelList.sort((a, b) => (b.createTime ?? 0) - (a.createTime ?? 0));
  }

  void initEventBus() {
    _subscription = eventBus.on<ModelMessageEvent>().listen((event) {
      if (event.message == EventBusMessage.updateList) {
        loadData();
      }
    });
  }

  @override
  void onClose() {
    _subscription.cancel();
    super.onClose();
  }

  @override
  void onWindowClose() {
    Get.back();
  }

  void removeModel(String id) {
    _showDeleteConfirmationDialog(id);
  }

  void _showDeleteConfirmationDialog(String id) {
    Get.dialog(
      barrierDismissible: false,
      CommonConfirmDialog(
        title: "删除确认",
        content: "即将删除模型的所有信息，确认删除？",
        confirmString: "删除",
        onConfirmCallback: () async {
          await _executeDelete(id);
        },
      ),
    );
  }

  Future<void> _executeDelete(String id) async {
    for (var model in modelList) {
      if (model.id == id) {
        modelList.remove(model);
        await modelRepository.removeModel(id);
        eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateSingleData, model: model));
        break;
      }
    }
  }

  Future<void> updateModel(String id, ModelFormData modelData) async {
    final targetModel = await _findOrCreateModel(id);
    if (targetModel != null) {
      _updateModelProperties(targetModel, modelData);
      await _saveModel(targetModel);
    }
  }

  Future<ModelData?> _findOrCreateModel(String id) async {
    if (id.isNotEmpty) {
      for (var model in modelList) {
        if (model.id == id) {
          return model;
        }
      }
    } else {
      final newModel = ModelData.newEmptyModel(id: snowFlakeUtil.getId(), createTime: DateTime.now().microsecondsSinceEpoch);
      modelList.add(newModel);
      return newModel;
    }
    return null;
  }

  void _updateModelProperties(ModelData targetModel, ModelFormData modelData) {
    targetModel.type = modelData.modelType;
    targetModel.name = modelData.name;
    targetModel.alias = modelData.alias;
    targetModel.key = modelData.apiKey;
    targetModel.url = modelData.baseUrl;
    targetModel.maxToken = modelData.maxToken;
    targetModel.supportMultiAgent = modelData.supportMultiAgent;
    targetModel.supportToolCalling = modelData.supportToolCalling;
    targetModel.supportDeepThinking = modelData.supportDeepThinking;
  }

  Future<void> _saveModel(ModelData targetModel) async {
    modelList.refresh();
    await modelRepository.updateModel(targetModel.id, targetModel);
    eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateSingleData, model: targetModel));
  }

  void showCreateModelDialog() {
    showEditModelDialog(null);
  }

  void showEditModelDialog(ModelData? model) {
    Get.dialog(
        barrierDismissible: false,
        EditModelDialog(
            model: model,
            isEdit: model != null,
            onConfirmCallback: (ModelFormData? modelData, {bool isDelete = false}) async {
              if (isDelete && model != null) {
                modelList.remove(model);
                await modelRepository.removeModel(model.id);
                eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateList));
              } else if (modelData != null) {
                updateModel(model?.id ?? "", modelData);
              }
            }));
  }

  void showDetailDialog(ModelData model) {
    Get.dialog(barrierDismissible: false, ModelDetailDialog(model: model));
  }

  void importModel() async {
    try {
      final fileResult = await ModelImportUtil.selectAndValidateImportFiles();
      if (fileResult == null) return;

      final allErrors = _collectImportErrors(fileResult);

      if (fileResult.validModels.isEmpty) {
        _showImportFailureDialog(allErrors);
        return;
      }

      if (allErrors.isEmpty) {
        await _performBatchImport(fileResult.validModels);
      } else {
        _showImportConfirmationDialog(allErrors, fileResult.validModels);
      }
    } catch (e) {
      AlarmUtil.showAlertDialog("导入失败");
    }
  }

  List<String> _collectImportErrors(ModelImportValidationResult fileResult) {
    List<String> allErrors = [];
    if (fileResult.errors.isNotEmpty) {
      allErrors.add("导入错误：\n${fileResult.errors.join('\n')}");
    }
    return allErrors;
  }

  void _showImportFailureDialog(List<String> allErrors) {
    Get.dialog(
      barrierDismissible: false,
      CommonConfirmDialog(title: "导入失败", content: allErrors.join('\n\n'), confirmString: '', onConfirmCallback: null),
    );
  }

  void _showImportConfirmationDialog(List<String> allErrors, List<ModelDTO> validModels) {
    String content = "以下模型将被跳过：\n\n${allErrors.join('\n\n')}\n\n是否继续导入其余 ${validModels.length} 个有效模型？";
    Get.dialog(
      barrierDismissible: false,
      CommonConfirmDialog(
        title: "导入验证提示",
        content: content,
        confirmString: "继续导入",
        onConfirmCallback: () async {
          await _performBatchImport(validModels);
        },
      ),
    );
  }

  Future<void> _performBatchImport(List<ModelDTO> modelsToImport) async {
    try {
      final count = await ModelImportUtil.importModels(modelsToImport);
      loadData(); // 刷新模型列表
      AlarmUtil.showAlertToast(modelsToImport.length > 1 ? "成功导入$count个模型" : "导入成功");
    } catch (e) {
      AlarmUtil.showAlertToast("导入失败");
    }
  }
}
