import 'dart:convert';
import 'package:crypto/crypto.dart' as crypto;

import 'package:yaml/yaml.dart';

import '../../repositories/account_repository.dart';

extension StringExtension on String {
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

  Future<String> fillPicLinkPrefixAsync() async {
    if (startsWith("http")) {
      return this;
    }
    String serverUrl = await accountRepository.getApiServerUrl();
    return "$serverUrl/v1/file/download?filename=$this";
  }

  String fillPicLinkPrefix() {
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

  bool isYaml() {
    try {
      loadYaml(this) as YamlMap;
      return true;
    } catch (e) {
      return false;
    }
  }

  String get lastSixChars => length > 6 ? substring(length - 6) : this;

  /// 去除前后空白（包括空格、换行符、制表符等）
  String trimmed() => replaceAll(RegExp(r'^\s+|\s+$'), '');

  /// 仅去除前导空白
  String trimFront() => replaceAll(RegExp(r'^\s+'), '');

  /// 仅去除尾部空白
  String trimEnd() => replaceAll(RegExp(r'\s+$'), '');

  /// 计算当前字符串的 MD5 十六进制字符串
  String toMd5Hex() {
    final bytes = utf8.encode(this);
    final digest = crypto.md5.convert(bytes);
    return digest.toString();
  }

  /// 去除文件名后缀（例如: "a/b/c.json" -> "a/b/c"）
  /// - 若无后缀或以点结尾，返回原字符串
  /// - 只删除最后一个点及其之后的内容
  String withoutExtension() {
    if (isEmpty) return this;
    final lastSlash = lastIndexOf('/');
    final lastBackslash = lastIndexOf('\\');
    final sep = lastSlash > lastBackslash ? lastSlash : lastBackslash;
    final dot = lastIndexOf('.');
    if (dot <= 0) return this; // 无点或点在首位
    if (sep >= 0 && dot < sep) return this; // 点在目录部分
    return substring(0, dot);
  }
}
