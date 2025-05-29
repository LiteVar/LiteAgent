import 'dart:async';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/uitl/snowflake_uitl.dart';
import 'package:lite_agent_client/repositories/model_repository.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/widgets/dialog/dialog_model_edit.dart';
import 'package:window_manager/window_manager.dart';

import '../../models/local_data_model.dart';
import '../../widgets/dialog/dialog_common_confirm.dart';

class ModelLogic extends GetxController with WindowListener {
  late StreamSubscription _subscription;

  var modelList = <ModelBean>[].obs;

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
    Get.dialog(
        barrierDismissible: false,
        CommonConfirmDialog(
          title: "删除确认",
          content: "即将删除模型的所有信息，确认删除？",
          confirmString: "删除",
          onConfirmCallback: () async {
            for (var model in modelList) {
              if (model.id == id) {
                modelList.remove(model);
                await modelRepository.removeModel(id);
                eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateSingleData, model: model));
                break;
              }
            }
          },
        ));
  }

  Future<void> updateModel(String id, String modelName, String url, String key, int maxToken) async {
    ModelBean? targetModel;
    if (id.isNotEmpty) {
      for (var model in modelList) {
        if (model.id == id) {
          targetModel = model;
          break;
        }
      }
    } else {
      targetModel = ModelBean();
      targetModel.id = snowFlakeUtil.getId();
      targetModel.createTime = DateTime.now().microsecondsSinceEpoch;
      modelList.add(targetModel);
    }
    if (targetModel != null) {
      targetModel.name = modelName;
      targetModel.key = key;
      targetModel.url = url;
      targetModel.maxToken = maxToken.toString();
      modelList.refresh();
      await modelRepository.updateModel(targetModel.id, targetModel);
      eventBus.fire(ModelMessageEvent(message: EventBusMessage.updateSingleData, model: targetModel));
    }
  }

  void showCreateModelDialog() {
    showEditModelDialog(null);
  }

  void showEditModelDialog(ModelBean? model) {
    Get.dialog(
        barrierDismissible: false,
        EditModelDialog(
            model: model,
            isEdit: model != null,
            onConfirmCallback: (String name, String baseUrl, String apiKey, int token) {
              updateModel(model?.id ?? "", name, baseUrl, apiKey, token);
            }));
  }
}
