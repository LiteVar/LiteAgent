import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'package:lite_agent_client/widgets/common_dropdown.dart';
import 'model_state_manager.dart';

/// 语音配置组件
/// 负责语音与文本转化配置的展示和交互
class VoiceConfigWidget extends StatelessWidget {
  final ModelStateManager modelStateManager;
  final bool isAutoAgent;
  final VoidCallback? onDataChanged;
  final Function(bool)? onTTSEnabled;
  final Function(bool)? onASREnabled;

  const VoiceConfigWidget({
    super.key,
    required this.modelStateManager,
    required this.isAutoAgent,
    this.onDataChanged,
    this.onTTSEnabled,
    this.onASREnabled,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const SizedBox(height: 10),
        InkWell(
          onTap: () => modelStateManager.toggleAudioExpanded(!modelStateManager.isAudioExpanded.value),
          child: Row(children: [
            Obx(() => buildAssetImage(modelStateManager.isAudioExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 18,
                const Color(0xff333333))),
            const Text("语音与文本转化配置", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
          ]),
        ),
        Obx(() => Offstage(
              offstage: !modelStateManager.isAudioExpanded.value,
              child: Container(
                margin: const EdgeInsets.only(left: 18),
                child: Column(
                  children: [
                    Container(
                      margin: const EdgeInsets.only(top: 10, bottom: 10),
                      child: const Text("开启文字转语音后，AI回复的信息中将显示语音播放功能\n开启语音转文字后，可以使用麦克风进行语音输入",
                          style: TextStyle(color: Color(0xff999999), fontSize: 12)),
                    ),
                    _buildTTSConfig(),
                    const SizedBox(height: 10),
                    _buildASRConfig(),
                  ],
                ),
              ),
            )),
        const SizedBox(height: 10),
        if (!isAutoAgent) horizontalLine(),
      ],
    );
  }

  Widget _buildTTSConfig() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: const EdgeInsets.only(top: 6),
          child: const Text("文字转语音(TTS)", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
        ),
        const Spacer(),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Obx(() => Checkbox(
                      value: modelStateManager.enableTextToSpeech.value,
                      activeColor: Colors.blue,
                      checkColor: Colors.white,
                      onChanged: (isCheck) {
                        modelStateManager.toggleTextToSpeech(isCheck ?? false);
                        // 只有开启TTS且有模型时才显示音频按钮
                        bool shouldEnable = (isCheck ?? false) && modelStateManager.currentTTSModelId.value.isNotEmpty;
                        onTTSEnabled?.call(shouldEnable);
                        onDataChanged?.call();
                      },
                    )),
                const Text("开启", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
              ],
            ),
            Obx(() => SimpleDropdown<String>(
                  selectedValue: getCurrentTTSModelValue(modelStateManager.currentTTSModelId.value),
                  items: modelStateManager.ttsModelList
                      .map((model) => DropdownItem<String>(value: model.id, text: model.alias ?? ""))
                      .toList(),
                  placeholder: "请选择TTS模型",
                  isEnabled: modelStateManager.enableTextToSpeech.value,
                  displayText: modelStateManager.currentTTSModel?.alias ?? "请选择TTS模型",
                  onChanged: (value) {
                    if (value != null) {
                      modelStateManager.selectTTSModel(value);
                      // 只有开启TTS且有模型时才显示音频按钮
                      bool shouldEnable = modelStateManager.enableTextToSpeech.value && value.isNotEmpty;
                      onTTSEnabled?.call(shouldEnable);
                      onDataChanged?.call();
                    }
                  },
                ))
          ],
        )
      ],
    );
  }

  Widget _buildASRConfig() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: const EdgeInsets.only(top: 6),
          child: const Text("语音转文字(ASR)", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
        ),
        const Spacer(),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Obx(() => Checkbox(
                      value: modelStateManager.enableSpeechToText.value,
                      activeColor: Colors.blue,
                      checkColor: Colors.white,
                      onChanged: (isCheck) {
                        modelStateManager.toggleSpeechToText(isCheck ?? false);
                        // 只有开启ASR且有模型时才启用语音输入
                        bool shouldEnable = (isCheck ?? false) && modelStateManager.currentASRModelId.value.isNotEmpty;
                        onASREnabled?.call(shouldEnable);
                        onDataChanged?.call();
                      },
                    )),
                const Text("开启", style: TextStyle(fontSize: 12, color: Color(0xff333333))),
              ],
            ),
            Obx(() => SimpleDropdown<String>(
                  selectedValue: getCurrentASRModelValue(modelStateManager.currentASRModelId.value),
                  items: modelStateManager.asrModelList
                      .map((model) => DropdownItem<String>(value: model.id, text: model.alias ?? ""))
                      .toList(),
                  placeholder: "请选择ASR模型",
                  isEnabled: modelStateManager.enableSpeechToText.value,
                  displayText: modelStateManager.currentASRModel?.alias ?? "请选择ASR模型",
                  onChanged: (value) {
                    if (value != null) {
                      modelStateManager.selectASRModel(value);
                      // 只有开启ASR且有模型时才启用语音输入
                      bool shouldEnable = modelStateManager.enableSpeechToText.value && value.isNotEmpty;
                      onASREnabled?.call(shouldEnable);
                      onDataChanged?.call();
                    }
                  },
                ))
          ],
        )
      ],
    );
  }

  String? getCurrentASRModelValue(String asrModelId) {
    var ids = modelStateManager.asrModelList.map((model) => model.id).toList();
    if (ids.contains(asrModelId)) {
      return asrModelId;
    }
    return null;
  }

  String? getCurrentTTSModelValue(String ttsModelId) {
    var ids = modelStateManager.ttsModelList.map((model) => model.id).toList();
    if (ids.contains(ttsModelId)) {
      return ttsModelId;
    }
    return null;
  }
}
