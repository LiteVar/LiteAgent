import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

/// Agent参数弹窗组件
/// 负责显示和编辑Agent的参数设置
class AgentParamsPopup extends StatelessWidget {
  final ModelData? currentModel;
  final RxDouble sliderTempValue;
  final RxDouble sliderTokenValue;
  final RxDouble sliderTopPValue;
  final VoidCallback? onDataChanged;

  const AgentParamsPopup({
    super.key,
    this.currentModel,
    required this.sliderTempValue,
    required this.sliderTokenValue,
    required this.sliderTopPValue,
    this.onDataChanged,
  });

  @override
  Widget build(BuildContext context) {
    return DropdownButtonHideUnderline(
      child: DropdownButton2(
        customButton: buildCommonTextButton("更多", 32, 16, null),
        isExpanded: true,
        dropdownStyleData: DropdownStyleData(
          width: 360,
          offset: const Offset(0, -10),
          padding: const EdgeInsets.symmetric(vertical: 0),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey),
            borderRadius: const BorderRadius.all(Radius.circular(4)),
            color: Colors.white,
          ),
        ),
        menuItemStyleData: MenuItemStyleData(
          height: 150,
          overlayColor: WidgetStateProperty.resolveWith<Color>((Set<WidgetState> states) => Colors.transparent),
        ),
        items: [DropdownMenuItem<String>(value: "params", child: _buildParamsContent())],
        onChanged: (value) {},
      ),
    );
  }

  Widget _buildParamsContent() {
    return Column(
      children: [
        Obx(() => _buildSliderRow("temperature", 1.0, 0.0, true, sliderTempValue)),
        Obx(() {
          String maxTokenString = currentModel?.maxToken ?? "4096";
          int maxTokenLimit = int.parse(maxTokenString);
          return _buildSliderRow("maxToken", maxTokenLimit.toDouble(), 1, false, sliderTokenValue);
        }),
        Obx(() => _buildSliderRow("topP", 1.0, 0.0, true, sliderTopPValue)),
      ],
    );
  }

  Widget _buildSliderRow(String title, double maxValue, double minValue, bool needDecimal, Rx<double> sliderValue) {
    String outputValueString = needDecimal ? sliderValue.value.toStringAsFixed(1).toString() : sliderValue.value.toInt().toString();
    return Row(
      children: [
        SizedBox(width: 75, child: Text(title, style: const TextStyle(fontSize: 12, color: Color(0xff333333)))),
        Expanded(
          child: Slider(
            value: sliderValue.value,
            min: minValue,
            max: maxValue,
            activeColor: const Color(0xff2A82E4),
            inactiveColor: const Color(0xff999999),
            thumbColor: const Color(0xff2A82E4),
            onChanged: (double value) {
              sliderValue.value = value;
              onDataChanged?.call();
            },
          ),
        ),
        Container(
          width: 48,
          height: 26,
          decoration: BoxDecoration(
            border: Border.all(color: const Color(0xff333333)),
            borderRadius: const BorderRadius.all(Radius.circular(4)),
          ),
          child: Center(
            child: Text(
              outputValueString,
              style: const TextStyle(fontSize: 12, color: Color(0xff333333)),
            ),
          ),
        ),
      ],
    );
  }
}
