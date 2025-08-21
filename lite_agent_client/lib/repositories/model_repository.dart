import 'dart:convert';

import 'package:get/get.dart';
import 'package:hive/hive.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/server/api_server/model_server.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

import '../models/local_data_model.dart';
import 'account_repository.dart';
import '../utils/log_util.dart';

final modelRepository = ModelRepository();

class ModelRepository {
  static final ModelRepository _instance = ModelRepository._internal();

  factory ModelRepository() => _instance;

  ModelRepository._internal();

  static const String modelBoxKey = "model_box_key";
  Box<ModelBean>? _box;

  Future<Box<ModelBean>> get _modelBox async => _box ??= await Hive.openBox<ModelBean>(modelBoxKey);

  Future<Iterable<ModelBean>> getModelListFromBox() async {
    return (await _modelBox).values;
  }

  Future<void> removeModel(String key) async {
    await (await _modelBox).delete(key);
    if (await accountRepository.isLogin()) {
      await ModelServer.removeModel(key);
    }
  }

  Future<void> updateModel(String key, ModelBean llm) async {
    await (await _modelBox).put(key, llm);
    if (await accountRepository.isLogin()) {
      await uploadToServer([llm]);
    }
    Log.d("updateModel:$key");
  }

  Future<ModelBean?> getModelFromBox(String key) async {
    return (await _modelBox).get(key);
  }

  Future<void> clear() async {
    await (await _modelBox).clear();
  }

  Future<void> uploadToServer(List<ModelBean> models) async {
    List<Map<String, dynamic>> list = [];
    var iterable = models.iterator;
    while (iterable.moveNext()) {
      var model = iterable.current;
      if (!model.id.isNumericOnly) {
        continue;
      }
      String type = model.type ?? "LLM";
      if (type != "LLM") {
        type = type.toLowerCase();
      }
      String nickName = model.nickName ?? "模型${model.id.lastSixChars ?? ""}";
      var jsonMap = {
        "id": model.id,
        "alias": nickName,
        "name": model.name,
        "baseUrl": model.url,
        "apiKey": model.key,
        "type": type,
        "autoAgent": model.supportMultiAgent ?? false,
        "toolInvoke": model.supportToolCalling ?? true,
        "deepThink": model.supportDeepThinking ?? false,
      };
      list.add(jsonMap);
    }
    String jsonString = json.encode(list);
    List<dynamic> jsonArray = json.decode(jsonString);
    var response = await ModelServer.modelSync(jsonArray);
    if (response.code == 200) {
      Log.i("modelUploadServer:${list.length}");
    }
  }

  Future<void> uploadAllToServer() async {
    var models = <ModelBean>[];
    models.addAll(((await _modelBox).values));
    uploadToServer(models);
  }

  Future<List<ModelDTO>> getCloudAutoAgentModelList() async {
    List<ModelDTO> list = [];
    var response = await ModelServer.getAutoAgentModelList();
    if (response.data != null) {
      list.addAll(response.data!.list);
    }
    return list;
  }
}
