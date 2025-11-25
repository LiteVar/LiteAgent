import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'model_state_manager.dart';

/// 模型选择器组件
/// 负责模型的选择
class ModelSelector extends StatelessWidget {
  final ModelStateManager modelStateManager;
  final Function(String) onModelSelected;
  final VoidCallback onCreateModel;

  const ModelSelector({
    super.key,
    required this.modelStateManager,
    required this.onModelSelected,
    required this.onCreateModel,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 36,
      decoration: const BoxDecoration(
        color: Color(0xfff5f5f5),
        borderRadius: BorderRadius.all(Radius.circular(4)),
      ),
      child: Center(
        child: Obx(() {
          var newButtonTag = "newButton";
          var list = <ModelData>[];
          list.assignAll(modelStateManager.llmModelList);
          ModelData newButton = ModelData.newEmptyModel(id: "", createTime: 0);
          newButton.id = newButtonTag;
          newButton.alias = "新建模型";
          list.add(newButton);
          var selectId = modelStateManager.currentLLMModelId.value;

          return DropdownButtonHideUnderline(
            child: DropdownButton2(
              isExpanded: true,
              items: list.map<DropdownMenuItem<String>>((ModelData item) {
                var textColor = item.id != newButtonTag ? const Color(0xff333333) : const Color(0xff2A82E4);
                String nickName = item.alias ?? "";
                return DropdownMenuItem<String>(
                    value: item.id,
                    child: Text(nickName, style: TextStyle(fontSize: 14, color: textColor), maxLines: 1, overflow: TextOverflow.ellipsis));
              }).toList(),
              value: selectId.isEmpty ? null : selectId,
              onChanged: (value) {
                if (value != null && value != newButtonTag) {
                  onModelSelected(value);
                } else {
                  onCreateModel();
                }
              },
              dropdownStyleData:
                  const DropdownStyleData(offset: Offset(0, -10), maxHeight: 200, decoration: BoxDecoration(color: Colors.white)),
            ),
          );
        }),
      ),
    );
  }
}
