import 'package:json_annotation/json_annotation.dart';

part 'dto.g.dart';

@JsonSerializable()
class VersionDto {
  late String version;

  VersionDto({required this.version});

  factory VersionDto.fromJson(Map<String, dynamic> json) =>
      _$VersionDtoFromJson(json);

  Map<String, dynamic> toJson() => _$VersionDtoToJson(this);
}

// @JsonSerializable(createFactory: false)
// class ExceptionMessageDto implements Exception {
//   final int code;
//   final String message;
//
//   ExceptionMessageDto({
//     required this.code,
//     required this.message,
//   });
//
//   Map<String, dynamic> toJson() => _$ExceptionMessageDtoToJson(this);
// }