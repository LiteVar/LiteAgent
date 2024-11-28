import 'dart:convert';
import 'dart:io';

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

  bool isNumeric() {
    return RegExp(r'^\d+$').hasMatch(this);
  }

  Future<String> fillPicLinkPrefix() async {
    String serverUrl = await accountRepository.getApiServerUrl();
    return "$serverUrl/v1/file/download?filename=$this";
  }
}
