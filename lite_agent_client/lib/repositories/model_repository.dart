import 'dart:convert';

import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/server/api_server/model_server.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';

import '../utils/log_util.dart';
import 'account_repository.dart';
import 'base_hive_repository.dart';

final modelRepository = ModelRepository();

class ModelRepository extends BaseHiveRepository<ModelData> {
  static final ModelRepository _instance = ModelRepository._internal();

  factory ModelRepository() => _instance;

  ModelRepository._internal() : super("model_box_key");

  Future<Iterable<ModelData>> getModelListFromBox() async {
    final models = await getAll();
    final needUpdate = <ModelData>[];

    for (final model in models) {
      if (model.alias?.isEmpty != false) {
        model.alias = "模型${model.id.lastSixChars}";
        needUpdate.add(model);
      }
    }

    if (needUpdate.isNotEmpty) {
      final updateMap = {for (final model in needUpdate) model.id: model};
      await saveAll(updateMap);
    }

    return models;
  }

  Future<void> removeModel(String key) async {
    await delete(key);
    if (await accountRepository.isLogin()) {
      await ModelServer.removeModel(key);
    }
  }

  Future<void> updateModel(String key, ModelData llm) async {
    await save(key, llm);
    if (await accountRepository.isLogin()) {
      await uploadToServer([llm]);
    }
  }

  Future<void> updateModels(Map<String, ModelData> models) async {
    await saveAll(models);
    if (await accountRepository.isLogin()) {
      await uploadToServer(models.values.toList());
    }
  }

  Future<ModelData?> getModelFromBox(String key) async => getData(key);

  Future<void> uploadToServer(List<ModelData> models) async {
    List<Map<String, dynamic>> list = [];
    var iterable = models.iterator;
    while (iterable.moveNext()) {
      var model = iterable.current;
      if (!model.id.isNumericOnly) {
        continue;
      }
      String type = model.type ?? ModelValidator.LLM;
      if (type != ModelValidator.LLM) {
        type = type.toLowerCase();
      }
      String nickName = model.alias ?? "模型${model.id.lastSixChars}";
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

  Future<void> importToServer(ModelDTO model) async {
    var jsonMap = model.toJson();
    var response = await ModelServer.importModel(jsonMap);
  }

  Future<void> uploadAllToServer() async {
    var models = <ModelData>[];
    models.addAll(await getAll());
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
