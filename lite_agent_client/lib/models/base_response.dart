class BaseResponse<T> {
  T data;
  int? code;
  String? message;

  BaseResponse({
    required this.data,
    this.code,
    this.message,
  });

  factory BaseResponse.fromJson(Map<String, dynamic> map, T Function(Map<String, dynamic>) fromJsonT) {
    dynamic data;
    if (map['data'] != null) {
      data = fromJsonT(map['data']);
    }
    return BaseResponse(
      data: data,
      code: map['code'],
      message: map['message'],
    );
  }

  factory BaseResponse.fromJsonForString(Map<String, dynamic> map) {
    return BaseResponse(
      data: map['data'],
      code: map['code'],
      message: map['message'],
    );
  }

  factory BaseResponse.fromJsonForList(Map<String, dynamic> map, T Function(List) fromJsonT) {
    dynamic list;
    if (map['data'] != null) {
      list = fromJsonT(map['data']);
    }
    return BaseResponse<T>(
      data: list,
      code: map['code'],
      message: map['message'],
    );
  }
}
