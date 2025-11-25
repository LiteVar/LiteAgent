import 'package:hive/hive.dart';
import 'package:json_annotation/json_annotation.dart';

import '../../config/constants.dart';

part 'model.g.dart';

@JsonSerializable()
@HiveType(typeId: HiveTypeIds.modelTypeId)
class ModelData extends Object {
  @HiveField(0)
  String id = "";
  @HiveField(1)
  String name = "";
  @HiveField(2)
  String url = "";
  @HiveField(3)
  String key = "";
  @HiveField(4)
  String? maxToken = "";
  @HiveField(5)
  int? createTime = 0;
  @HiveField(6)
  String? type = "";
  @HiveField(7)
  String? alias = "";
  @HiveField(8)
  bool? supportMultiAgent;
  @HiveField(9)
  bool? supportToolCalling;
  @HiveField(10)
  bool? supportDeepThinking;

  ModelData({
    required this.id,
    required this.name,
    required this.url,
    required this.key,
    this.maxToken = "4096",
    this.createTime = 0,
    required this.type,
    required this.alias,
    this.supportMultiAgent = false,
    this.supportToolCalling = true,
    this.supportDeepThinking = false,
  });

  factory ModelData.simpleConfig({required String name, required baseUrl, required String apiKey}) {
    return ModelData(id: "", name: name, url: baseUrl, key: apiKey, type: "", alias: "");
  }

  factory ModelData.newEmptyModel({required String id, required int createTime}) {
    return ModelData(id: id, name: "", url: "", key: "", type: "", alias: "", createTime: createTime);
  }


  factory ModelData.fromJson(Map<String, dynamic> srcJson) => _$ModelDataFromJson(srcJson);

  Map<String, dynamic> toJson() => _$ModelDataToJson(this);
}
