import 'dart:convert';
import 'dart:io';

import 'package:openapi_dart/openapi_dart.dart';
import 'package:openmodbus_dart/openmodbus_dart.dart';
import 'package:openrpc_dart/openrpc_dart.dart';
import 'package:opentool_dart/opentool_dart.dart';
import 'package:yaml/yaml.dart';

import '../../repositories/account_repository.dart';

extension StringHomeAbbreviation on String {
  // $HOME之前的路径(包括磁盘)缩写为~
  String homeAbbreviation() {
    String homeDirectory = Platform.environment['HOME'] ?? '';
    int startIndex = indexOf(homeDirectory);
    if (homeDirectory.isNotEmpty && startIndex >= 0) {
      return '~${substring(startIndex + homeDirectory.length)}';
    }
    return this;
  }

  // 删除$HOME之前的路径
  String homePrefix() {
    String homeDirectory = Platform.environment['HOME'] ?? '';
    int startIndex = indexOf(homeDirectory);
    if (homeDirectory.isNotEmpty && startIndex >= 0) {
      return substring(startIndex);
    }
    return this;
  }

  String nameOfPath() {
    final parts = split('/');
    final str = parts.isNotEmpty ? parts.last : '';
    return str.isNotEmpty ? str : this;
  }

  // 将String进行Base64编码
  String encodeBase64() {
    var bytes = utf8.encode(this); // 将当前字符串转换为字节
    return base64.encode(bytes); // 对字节进行Base64编码
  }

  // 解码Base64编码的String
  String decodeBase64() {
    var bytes = base64.decode(this); // 对当前Base64编码的字符串进行解码
    return utf8.decode(bytes); // 将字节转换回字符串
  }

  Future<String> fillPicLinkPrefix() async {
    if (startsWith("http")) {
      return this;
    }
    String serverUrl = await accountRepository.getApiServerUrl();
    return "$serverUrl/v1/file/download?filename=$this";
  }

  String fillPicLinkPrefixNoAsync() {
    if (startsWith("http") || isEmpty) {
      return this;
    }
    String serverUrl = accountRepository.getApiServerUrlNoAsync();
    return "$serverUrl/v1/file/download?filename=$this";
  }

  bool isJson() {
    try {
      json.decode(this) as Map<String, dynamic>;
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<bool> isOpenAIJson() async {
    try {
      json.decode(this) as Map<String, dynamic>;
      await OpenAPILoader().load(this);
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<bool> isOpenModBusJson() async {
    try {
      json.decode(this) as Map<String, dynamic>;
      await OpenModbusLoader().load(this);
      return true;
    } catch (e) {
      return false;
    }
  }

  Future<bool> isOpenPPCJson() async {
    try {
      json.decode(this) as Map<String, dynamic>;
      await OpenRPCLoader().load(this);
      return true;
    } catch (e) {
      return false;
    }
  }

  bool isYaml() {
    try {
      loadYaml(this) as YamlMap;
      return true;
    } catch (e) {
      return false;
    }
  }

  bool isOpenAIYaml() {
    try {
      var yaml = loadYaml(this) as YamlMap;
      return yaml["openapi"] != null && yaml["info"] != null;
    } catch (e) {
      return false;
    }
  }

  Future<bool> isOpenToolJson() async {
    try {
      json.decode(this) as Map<String, dynamic>;
      await OpenToolLoader().load(this);
      return true;
    } catch (e) {
      return false;
    }
  }
}
