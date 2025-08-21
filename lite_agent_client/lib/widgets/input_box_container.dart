import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../utils/alarm_util.dart';
import '../utils/log_util.dart';
import 'common_widget.dart';
import 'volume_wave_painter.dart';

class InputBoxController extends GetxController {
  void Function(String content)? onSendButtonPress;
  Future<void> Function(File recordFile)? onAudioRecordFinish;

  final TextEditingController textController = TextEditingController();
  final FocusNode textInputFocusNode = FocusNode();
  final FocusNode keyboardFocusNode = FocusNode();
  AudioRecorder recorder = AudioRecorder();

  static const maxHistoryLength = 100; // 5秒 × 20次/秒（50ms间隔）
  final volume = 0.0.obs;
  final volumeHistory = <double>[].obs;
  int historyIndex = 0;

  var isAudioInputStatus = false.obs;
  var enableInput = true;
  var showAudioButton = false.obs;
  var isAudioRecording = false;
  var isAudioEnable = true;
  DateTime? recordStartTime;
  String unableInputReasonString = "";

  void switchInputWay(bool isAudio) {
    if (isAudio) {
      textController.text = "";
      keyboardFocusNode.requestFocus();
    }
    isAudioInputStatus.value = isAudio;
  }

  void sendMessage() {
    textInputFocusNode.requestFocus();
    var content = textController.text;
    if (content.isNotEmpty) {
      textController.text = "";
      if (onSendButtonPress != null) {
        onSendButtonPress!(content);
      }
    }
  }

  Future<void> switchRecordStatus(bool isRecording) async {
    if (!isAudioEnable || !isAudioInputStatus.value) {
      return;
    }
    if (isRecording == isAudioRecording) {
      //status not change
      return;
    }
    // 检查权限
    if (!await recorder.hasPermission()) {
      AlarmUtil.showAlertToast("麦克风权限被拒绝");
      return;
    }
    if (isRecording) {
      // 重置指针和缓冲区
      historyIndex = 0; // 新增重置逻辑
      volumeHistory.clear();
      volume.value = 0.0;

      isAudioRecording = isRecording;
      refreshInputUIStatus();

      final dir = await getTemporaryDirectory();
      final path = '${dir.path}/recording.wav';

      recordStartTime = DateTime.now();
      try {
        await recorder.start(const RecordConfig(encoder: AudioEncoder.wav), path: path);
        recorder.onAmplitudeChanged(const Duration(milliseconds: 50)).listen((amp) {
          final normalized = (amp.current + 50) / 50;
          updateVolume(normalized.clamp(0.0, 1.0));
        });
      } on Exception catch (e) {
        Log.e("_recorder.start error: $e");
        await resetAudioAudioRecorder();
      }
    } else {
      isAudioRecording = isRecording;
      refreshInputUIStatus();

      String? recordFilePath;
      try {
        if (recordStartTime != null) {
          double recordDuration = DateTime.now().difference(recordStartTime!).inMilliseconds / 1000;
          if (recordDuration < 1.0) {
            AlarmUtil.showAlertToast("录音时间太短，无法识别");
          }
        }

        recordFilePath = await recorder.stop();
      } on Exception catch (e) {
        Log.e("recorder.stop error: $e");
        await resetAudioAudioRecorder();
      }
      if (recordFilePath != null) {
        File recordFile = File(recordFilePath);
        if (onAudioRecordFinish != null) {
          await onAudioRecordFinish!(recordFile);
        }
        recordFile.delete();
      }
    }
  }

  void updateVolume(double newVolume) {
    volume.value = newVolume;

    // 环形缓冲区实现
    if (volumeHistory.length < maxHistoryLength) {
      volumeHistory.add(newVolume);
    } else {
      volumeHistory[historyIndex] = newVolume;
      historyIndex = (historyIndex + 1) % maxHistoryLength; // 循环指针
    }
  }

  Future<void> resetAudioAudioRecorder() async {
    await recorder.dispose();
    recorder = AudioRecorder();
  }

  void setEnableAudioInput(bool enable) {
    showAudioButton.value = enable;
    if (!enable) {
      isAudioInputStatus.value = false;
    }
  }

  void setEnableInput(bool enable, String reason) {
    enableInput = enable;
    if (!enable) {
      textController.text = "";
      unableInputReasonString = reason;
    }
    refreshInputUIStatus();
  }

  void setAudioEnable(bool enable) {
    isAudioEnable = enable;
  }

  void refreshInputUIStatus() {
    isAudioInputStatus.refresh();
  }

  // 资源释放
  @override
  void dispose() {
    onSendButtonPress = null;
    onAudioRecordFinish = null;
    textController.dispose();
    textInputFocusNode.dispose();
    keyboardFocusNode.dispose();
    recorder.stop();
    recorder.dispose();
    super.dispose();
  }
}

class InputBoxContainer extends StatelessWidget {
  final InputBoxController controller;

  InputBoxContainer({super.key, required this.controller});

  @override
  Widget build(BuildContext context) {
    controller.textController.addListener(() => controller.refreshInputUIStatus());
    return Container(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        margin: const EdgeInsets.only(bottom: 20),
        child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 0, vertical: 10),
            decoration: const BoxDecoration(color: Color(0xfff5f5f5), borderRadius: BorderRadius.all(Radius.circular(8))),
            child: Column(
              children: [
                buildInputConstrainedBox(),
                Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                  Obx(() {
                    if (controller.isAudioInputStatus.value && controller.enableInput) {
                      return InkWell(
                          onTap: () => controller.switchInputWay(false),
                          child: Container(
                              margin: const EdgeInsets.only(right: 4),
                              padding: const EdgeInsets.all(4),
                              child: SizedBox(
                                width: 44,
                                height: 28,
                                child: Image.asset("assets/images/icon_keyboard.png", fit: BoxFit.contain, color: null),
                              )));
                    } else {
                      var iconName = controller.textController.text.isEmpty ? "icon_cant_send_button.png" : "icon_send_button.png";
                      return Row(
                        children: [
                          Obx(() => Offstage(
                                offstage: !(controller.showAudioButton.value && controller.enableInput),
                                child: InkWell(
                                    onTap: () => controller.switchInputWay(true),
                                    child: Container(
                                        margin: const EdgeInsets.only(right: 4),
                                        padding: const EdgeInsets.all(4),
                                        child: buildAssetImage("icon_audio_record.png", 28, null))),
                              )),
                          InkWell(
                              onTap: () => controller.textController.text.isNotEmpty ? controller.sendMessage() : null,
                              child: Container(
                                margin: const EdgeInsets.only(right: 8),
                                padding: const EdgeInsets.all(4),
                                child: buildAssetImage(iconName, 28, null),
                              ))
                        ],
                      );
                    }
                  })
                ])
              ],
            )));
  }

  Widget buildInputConstrainedBox() {
    return RawKeyboardListener(
      focusNode: controller.keyboardFocusNode,
      autofocus: true,
      onKey: (RawKeyEvent event) {
        if (event is RawKeyDownEvent && event.logicalKey == LogicalKeyboardKey.space) {
          controller.switchRecordStatus(true);
        }
        if (event is RawKeyUpEvent && event.logicalKey == LogicalKeyboardKey.space) {
          controller.switchRecordStatus(false);
        }
      },
      child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 32, maxHeight: 200),
          child: Obx(() {
            if (!controller.isAudioInputStatus.value) {
              var enabled = controller.enableInput;
              return TextField(
                enabled: enabled,
                onSubmitted: (string) => controller.sendMessage(),
                focusNode: controller.textInputFocusNode,
                controller: controller.textController,
                cursorColor: const Color(0xff2A82E4),
                keyboardType: TextInputType.text,
                maxLines: null,
                minLines: 1,
                decoration: InputDecoration(
                    hintText: enabled ? '请输入聊天内容' : controller.unableInputReasonString,
                    border: InputBorder.none,
                    isDense: true,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4)),
                style: const TextStyle(fontSize: 14),
              );
            } else {
              return InkWell(
                onTapDown: (_) => controller.switchRecordStatus(true),
                onTapUp: (_) => controller.switchRecordStatus(false),
                onTapCancel: () => controller.switchRecordStatus(false),
                child: controller.isAudioRecording ? buildVolumeBar() : buildRecordButton(),
              );
            }
          })),
    );
  }

  Container buildRecordButton() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 10),
      height: 32,
      decoration: const BoxDecoration(color: Color(0xffDEDEDE), borderRadius: BorderRadius.all(Radius.circular(12))),
      child: const Center(child: Text("按住此处或者空格说话", style: TextStyle(fontSize: 12, color: Color(0xff333333)))),
    );
  }

  Container buildVolumeBar() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 10),
      height: 32,
      child: Obx(() => CustomPaint(
          size: Size.infinite,
          painter: VolumeWavePainter(
            volume: controller.volume.value,
            volumeHistory: controller.volumeHistory,
            historyIndex: controller.historyIndex,
            color: const Color(0xff2A82E4),
          ))),
    );
  }
}
