import 'package:hive/hive.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';

abstract class BaseHiveRepository<T> {
  final String boxKey;
  Box<T>? _box;

  BaseHiveRepository(this.boxKey);

  Future<Box<T>> get box async => _box ??= await Hive.openBox<T>(boxKey);

  Future<Iterable<T>> getAll() async => (await box).values;

  Future<T?> getData(String key) async => (await box).get(key);

  Future<void> save(String key, T data) async => (await box).put(key, data);

  Future<void> saveAll(Map<String, T> dataMap) async => (await box).putAll(dataMap);

  Future<void> delete(String key) async => (await box).delete(key);

  Future<void> deleteAll(List<String> keys) async => (await box).deleteAll(keys);

  Future<void> clear() async => (await box).clear();

  Future<bool> exists(String key) async => (await box).containsKey(key);

  Future<int> count() async => (await box).length;

  Future<Iterable<String>> getAllKeys() async => (await box).keys.cast<String>();

  Future<Iterable<T>> where(bool Function(T) test) async => (await box).values.where(test);

  Future<T?> firstWhere(bool Function(T) test, {T Function()? orElse}) async {
    try {
      return (await box).values.firstWhere(test, orElse: orElse);
    } catch (e) {
      return orElse?.call();
    }
  }

  Future<void> close() async {
    if (_box != null && _box!.isOpen) {
      await _box!.close();
      _box = null;
    }
  }
}
