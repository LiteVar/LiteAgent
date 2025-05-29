import 'dart:convert';

import 'package:hive/hive.dart';
import 'package:lite_agent_client/server/api_server/model_server.dart';

import '../models/local_data_model.dart';
import 'account_repository.dart';

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
    print("updateModel:$key");
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
      var jsonMap = {"id": model.id, "name": model.name, "baseUrl": model.url, "apiKey": model.key, "type": "text"};
      list.add(jsonMap);
    }
    String jsonString = json.encode(list);
    List<dynamic> jsonArray = json.decode(jsonString);
    //print("jsonString:$jsonString");
    var response = await ModelServer.modelSync(jsonArray);
    if (response.code == 200) {
      print("modelUploadServer:${list.length}");
    }
  }

  Future<void> uploadAllToServer() async {
    var models = <ModelBean>[];
    models.addAll(((await _modelBox).values));
    uploadToServer(models);
  }
}
