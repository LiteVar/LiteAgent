import 'package:lite_agent_client/models/dto/model.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/utils/model/model_converter.dart';
import 'package:lite_agent_client/utils/model/model_export.dart';
import 'package:lite_agent_client/utils/model/model_validator.dart';

extension ModelDTOExtensions on ModelDTO {
  /// DTO -> Model 转换（委托 ModelConverter）
  ModelData toModel() => ModelConverter.dtoToModel(this);

  /// 导出模型为JSON文件（委托 ModelExportUtil）
  Future<String?> exportJson(bool exportPlaintext) => ModelExportUtil.exportModelToJson(this, exportPlaintext: exportPlaintext);

  ///新建只有id的空ModelDto
  ModelDTO createOnlyIdModelDto(String id) {
    return ModelDTO(id, '', '', '', '', 0, '', false, false, false, '', 0);
  }
}

extension ModelDataExtensions on ModelData {
  /// Model -> DTO 转换（委托 ModelConverter）
  ModelDTO toDTO() => ModelConverter.modelToDto(this);
}
