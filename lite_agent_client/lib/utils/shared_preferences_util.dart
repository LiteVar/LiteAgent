import 'package:shared_preferences/shared_preferences.dart';

final SPUtil = SharedPreferencesUtil();

class SharedPreferencesUtil {
  static final SharedPreferencesUtil _instance = SharedPreferencesUtil._internal();

  factory SharedPreferencesUtil() => _instance;

  SharedPreferencesUtil._internal();

  SharedPreferences? _prefs;

  Future<SharedPreferences> get prefs async => _prefs ??= await SharedPreferences.getInstance();

  static const String serverUrl = 'serverUrl';
  static const String tokenKey = 'userToken';
  static const String userInfo = 'userInfo';
  static const String workspace = 'workspace';
  static const String password = 'password';
  static const String isAutoLogin = 'isAutoLogin';

  Future<void> setString(String key, String string) async {
    await (await prefs).setString(key, string);
  }

  Future<String?> getString(String key) async {
    return (await prefs).getString(key);
  }

  Future<void> remove(String key) async {
    await (await prefs).remove(key);
  }

  Future<bool> isTrue(String key) async {
    return ((await prefs).getBool(key)) ?? false;
  }

  Future<void> setBool(String key, bool value) async {
    await (await prefs).setBool(key, value);
  }

  Future<int?> getInt(String key) async {
    return ((await prefs).getInt(key));
  }

  Future<void> setInt(String key, int value) async {
    await (await prefs).setInt(key, value);
  }
}
