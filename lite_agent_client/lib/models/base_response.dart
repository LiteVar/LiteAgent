import '../utils/log_util.dart';

class BaseResponse<T> {
  T? data;
  int? code;
  String? message;

  BaseResponse({
    required this.data,
    this.code,
    this.message,
  });

  factory BaseResponse.fromJson(Map<String, dynamic>? map, T Function(Map<String, dynamic>) fromJsonT) {
    if (map == null) {
      return BaseResponse(data: null, code: -1, message: "unknown error");
    }
    try {
      dynamic data;
      if (map['data'] != null) {
        data = fromJsonT(map['data']);
      }
      return BaseResponse(
        data: data,
        code: map['code'],
        message: map['message'],
      );
    } catch (e) {
      Log.e("BaseResponse.fromJson error: $e");
      return BaseResponse(data: null, code: -2, message: "fromJson error");
    }
  }

  factory BaseResponse.fromJsonForString(Map<String, dynamic>? map) {
    if (map == null) {
      return BaseResponse(data: null, code: -1, message: "unknown error");
    }
    try {
      return BaseResponse(
        data: map['data'],
        code: map['code'],
        message: map['message'],
      );
    } catch (e) {
      Log.e("BaseResponse.fromJsonForString error: $e");
      return BaseResponse(data: null, code: -2, message: "fromJson error");
    }
  }

  factory BaseResponse.fromJsonForList(Map<String, dynamic>? map, T Function(List) fromJsonT) {
    if (map == null) {
      return BaseResponse(data: null, code: -1, message: "unknown error");
    }
    try {
      dynamic list;
      if (map['data'] != null) {
        list = fromJsonT(map['data']);
      }
      return BaseResponse<T>(
        data: list,
        code: map['code'],
        message: map['message'],
      );
    } catch (e) {
      Log.e("BaseResponse.fromJsonForList error: $e");
      return BaseResponse(data: null, code: -2, message: "fromJson error");
    }
  }
}
