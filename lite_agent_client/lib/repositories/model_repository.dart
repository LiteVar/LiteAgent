import 'package:hive/hive.dart';

import '../models/local_data_model.dart';

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
  }

  Future<void> updateModel(String key, ModelBean llm) async {
    await (await _modelBox).put(key, llm);
  }

  Future<ModelBean?> getModelFromBox(String key) async {
    return (await _modelBox).get(key);
  }

  Future<void> clear() async {
    await (await _modelBox).clear();
  }
}
