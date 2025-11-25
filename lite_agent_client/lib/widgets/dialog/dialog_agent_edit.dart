import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/agent.dart';
import 'package:lite_agent_client/utils/agent/agent_validator.dart';
import '../../utils/log_util.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../utils/file_util.dart';

class EditAgentDialog extends StatefulWidget {
  final AgentModel? agent;
  final bool isEdit;
  final void Function(String name, String iconPath, String description) onConfirmCallback;

  const EditAgentDialog({
    super.key,
    required this.agent,
    required this.isEdit,
    required this.onConfirmCallback,
  });

  @override
  State<EditAgentDialog> createState() => _EditToolDialogState();
}

class _EditToolDialogState extends State<EditAgentDialog> {
  final itemBorderColor = const Color(0xFFd9d9d9);

  TextEditingController? _nameController;
  TextEditingController? _desController;
  final _iconPath = "".obs;

  void _initData() {
    _nameController = TextEditingController(text: widget.agent?.name ?? "");
    _iconPath.value = widget.agent?.iconPath ?? "";
    _desController = TextEditingController(text: widget.agent?.description ?? "");
  }

  Future<void> _confirm() async {
    String name = _nameController!.text;
    String description = _desController!.text;
    if (name.trim().isEmpty) {
      AlarmUtil.showAlertDialog("Agent名称必填项不能为空");
      return;
    }
    if (!await AgentValidator.isNameUniqueAsync(name, excludeId: widget.agent?.id)) {
      AlarmUtil.showAlertDialog("Agent名称已存在，请重新输入");
      return;
    }
    widget.onConfirmCallback(name, _iconPath.value, description);
    Get.back();
  }

  @override
  void dispose() {
    _nameController?.dispose();
    _desController?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    _initData();
    return Center(
      child: Container(
        width: 538,
        height: 596,
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
        child: Column(
          children: [
            _buildTitleContainer(),
            Expanded(
              child: SingleChildScrollView(
                child: Container(
                  margin: const EdgeInsets.all(16),
                  child: Column(
                    children: [
                      _buildToolDesColumn(),
                      const SizedBox(height: 10),
                      _buildBottomButton(),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Row _buildBottomButton() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(2), side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0)),
                )),
            onPressed: () => Get.back(),
            child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14))),
        const SizedBox(width: 16),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(borderRadius: BorderRadius.circular(2)),
                )),
            onPressed: () => _confirm(),
            child: const Text('确定', style: TextStyle(color: Colors.white, fontSize: 14)))
      ],
    );
  }

  Column _buildToolDesColumn() {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      const Row(
        children: [
          Text("Agent名称", style: TextStyle(fontSize: 14, color: Colors.black)),
          SizedBox(width: 10),
          Text("*", style: TextStyle(fontSize: 14, color: Colors.red))
        ],
      ),
      Container(
          height: 40,
          margin: const EdgeInsets.symmetric(vertical: 16),
          padding: const EdgeInsets.symmetric(horizontal: 8),
          decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(2)),
          child: Center(
            child: TextField(
                controller: _nameController,
                cursorColor: Colors.grey,
                maxLines: 1,
                maxLength: 20,
                decoration: const InputDecoration(
                  hintText: '请输入Agent名称',
                  border: InputBorder.none,
                  isDense: true,
                  contentPadding: EdgeInsets.symmetric(horizontal: 8),
                  counterText: "",
                ),
                style: const TextStyle(fontSize: 14)),
          )),
      const Text("图标", style: TextStyle(fontSize: 14, color: Colors.black)),
      Container(
        margin: const EdgeInsets.symmetric(vertical: 16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            SizedBox(width: 82, height: 82, child: Obx(() => buildAgentProfileImage(_iconPath.value))),
            const SizedBox(width: 10),
            Obx(() {
              return Row(children: [
                InkWell(
                  onTap: () {
                    selectImgFile();
                  }.throttle(),
                  child: const Text('选择图标', style: TextStyle(fontSize: 14, color: Colors.blue)),
                ),
                const SizedBox(width: 10),
                if (_iconPath.value.isNotEmpty)
                  InkWell(
                    onTap: () => _iconPath.value = "",
                    child: const Text('恢复默认', style: TextStyle(fontSize: 14, color: Colors.blue)),
                  ),
              ]);
            })
          ],
        ),
      ),
      const Text("描述", style: TextStyle(fontSize: 14, color: Colors.black)),
      Container(
        margin: const EdgeInsets.symmetric(vertical: 16),
        padding: const EdgeInsets.symmetric(horizontal: 8),
        height: 188,
        decoration: BoxDecoration(border: Border.all(color: itemBorderColor), borderRadius: BorderRadius.circular(2)),
        child: TextField(
            controller: _desController,
            maxLines: null,
            maxLength: 200,
            minLines: 1,
            decoration: const InputDecoration(
              hintText: '用简单几句话将Agent介绍给用户',
              border: InputBorder.none,
              contentPadding: EdgeInsets.all(8),
              counterText: "",
            ),
            style: const TextStyle(fontSize: 14)),
      )
    ]);
  }

  Container _buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        Text(widget.isEdit ? "编辑本地Agent" : "新建本地Agent"),
        const Spacer(),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () => Get.back(),
        )
      ]),
    );
  }

  /*Future<void> uploadImgFile() async {
    var result = await FilePicker.platform.pickFiles(type: FileType.image);
    if (result != null && result.files.single.path != null) {
      Log.d("path:${result.files.single.path}");
      String imgPath = result.files.single.path ?? "";
      if (imgPath.isNotEmpty) {
        EasyLoading.show();
        try {
          BaseResponse<String?> response = await FileServer.uploadFile(imgPath);
          EasyLoading.dismiss();
          if (response.code == 200) {
            String fileName = response.data ?? "";
            if (fileName.isNotEmpty) {
              _iconPath.value = await fileName.fillPicLinkPrefix();
            }
          }
        } catch (e) {
          EasyLoading.dismiss();
          Log.e("上传失败");
        }
      }
    }
  }*/

  Future<void> selectImgFile() async {
    String path = await fileUtils.selectImgFile() ?? "";
    if (path.isNotEmpty) {
      EasyLoading.show();
      File imageFile = File(path);
      var size = imageFile.readAsBytesSync().length / 1024;
      if (size > (1024 * 2)) {
        AlarmUtil.showAlertToast("图片大小不能超过2M");
        EasyLoading.dismiss();
        return;
      }

      String? iconPath = await fileUtils.processImage(imageFile);
      if (iconPath != null && iconPath.isNotEmpty) {
        _iconPath.value = "${Constants.localFilePrefix}$iconPath";
        _iconPath.refresh();
      }
      EasyLoading.dismiss();
    }
  }
}
