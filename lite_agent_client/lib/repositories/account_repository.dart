import 'dart:convert';

import 'package:get/get_utils/get_utils.dart';
import 'package:lite_agent_client/models/dto/account.dart';
import 'package:lite_agent_client/models/dto/workspace.dart';
import 'package:lite_agent_client/server/api_server/workspace_server.dart';
import 'package:lite_agent_client/utils/event_bus.dart';
import 'package:lite_agent_client/utils/shared_preferences_uitl.dart';

import '../server/api_server/account_server.dart';

final accountRepository = AccountRepository();

class AccountRepository {
  static final AccountRepository _instance = AccountRepository._internal();

  factory AccountRepository() => _instance;

  AccountRepository._internal();

  String _token = "";
  String _serverUrl = "";
  String _currentWorkSpaceId = "";

  Future<void> setServerUrl(String serverUrl) async {
    _serverUrl = serverUrl;
    await SPUtil.setString(SharedPreferencesUtil.serverUrl, serverUrl);
  }

  Future<String> getApiServerUrl() async {
    if (_serverUrl.isEmpty) {
      _serverUrl = await SPUtil.getString(SharedPreferencesUtil.serverUrl) ?? "";
    }
    return _serverUrl;
  }

  String getApiServerUrlNoAsync() {
    return _serverUrl;
  }

  Future<void> setToken(String token) async {
    _token = token;
    await SPUtil.setString(SharedPreferencesUtil.tokenKey, token);
  }

  Future<String> getApiToken() async {
    if (_token.isEmpty) {
      _token = await SPUtil.getString(SharedPreferencesUtil.tokenKey) ?? "";
    }
    if (_token.isNotEmpty) {
      return "Bearer $_token";
    }
    return "";
  }

  Future<void> setWorkSpace(WorkSpaceDTO workspace) async {
    _currentWorkSpaceId = workspace.id;
    await SPUtil.setString(SharedPreferencesUtil.workspace, jsonEncode(workspace));
  }

  Future<String> getWorkSpaceId() async {
    if (_currentWorkSpaceId.isEmpty) {
      String jsonString = await SPUtil.getString(SharedPreferencesUtil.workspace) ?? "";
      if (jsonString.isNotEmpty) {
        try {
          Map<String, dynamic> jsonMap = json.decode(jsonString);
          var workspace = WorkSpaceDTO.fromJson(jsonMap);
          return workspace.id;
        } catch (e) {
          e.printError();
        }
      }
    }
    return _currentWorkSpaceId;
  }

  Future<void> login(String email, String password, String server) async {
    var response = await AccountServer.login(email, password, server);
    if (response.code == 200) {
      String token = response.data ?? "";
      if (token.isNotEmpty) {
        await setToken(token);
        await setServerUrl(server);
        await accountRepository.getAccountWorkspaceList();
        eventBus.fire(MessageEvent(message: EventBusMessage.login));
      }
    }
  }

  Future<AccountDTO?> updateUserInfoFromNet() async {
    var response = await AccountServer.getUserInfo();
    if (response.code == 10003) {
      //无效token
      await clearLoginInfo();
    }
    if (response.data != null) {
      updateAccount(response.data);
      eventBus.fire(MessageEvent(message: EventBusMessage.updateSingleData, data: response.data));
      return response.data;
    }
    return null;
  }

  Future<void> updateAccount(AccountDTO? account) async {
    if (account == null) {
      return;
    }
    var json = jsonEncode(account);
    await SPUtil.setString(SharedPreferencesUtil.userInfo, json);
  }

  Future<AccountDTO?> getAccountInfoFromBox() async {
    String jsonString = await SPUtil.getString(SharedPreferencesUtil.userInfo) ?? "";
    try {
      if (jsonString.isNotEmpty) {
        Map<String, dynamic> jsonMap = json.decode(jsonString);
        return AccountDTO.fromJson(jsonMap);
      }
    } catch (e) {
      e.printError();
    }
    return null;
  }

  Future<List<WorkSpaceDTO>?> getAccountWorkspaceList() async {
    var response = await WorkSpaceServer.getWorkspaceList();
    if (_currentWorkSpaceId.isEmpty && response.data != null && response.data!.isNotEmpty) {
      setWorkSpace(response.data![0]);
    }
    return response.data;
  }

  Future<bool> isLogin() async {
    return (await getApiToken()).isNotEmpty;
  }

  Future<void> logout() async {
    var response = await AccountServer.logout();
    //if (response.code == 200) {
      await clearLoginInfo();
    //}
  }

  Future<void> clearLoginInfo() async {
    _token = "";
    _currentWorkSpaceId = "";
    await SPUtil.remove(SharedPreferencesUtil.tokenKey);
    //await SPUtil.remove(SharedPreferencesUtil.serverUrl);
    eventBus.fire(MessageEvent(message: EventBusMessage.logout));
    //await spUtil.remove(SPUtil.userId);
  }
}
