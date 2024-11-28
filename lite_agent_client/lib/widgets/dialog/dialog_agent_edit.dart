import 'dart:io';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/utils/file_util.dart';

class EditAgentDialog extends StatefulWidget {
  final String name;
  final String iconPath;
  final String description;
  final bool isEdit;
  final void Function(String name, String iconPath, String description) onConfirmCallback;

  const EditAgentDialog({
    super.key,
    required this.name,
    required this.iconPath,
    required this.description,
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
    _nameController = TextEditingController(text: widget.name);
    _iconPath.value = widget.iconPath;
    _desController = TextEditingController(text: widget.description);
  }

  void _confirm() {
    String name = _nameController!.text;
    String description = _desController!.text;
    if (name.trim().isEmpty) {
      _showAlertDialog();
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
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.all(Radius.circular(6)),
        ),
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
            ))),
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
                    borderRadius: BorderRadius.circular(2),
                    side: const BorderSide(color: Color(0xFFd9d9d9), width: 1.0),
                  ),
                )),
            onPressed: () {
              Get.back();
            },
            child: const Text('取消', style: TextStyle(color: Color(0xFF999999), fontSize: 14))),
        const SizedBox(width: 16),
        TextButton(
            style: ButtonStyle(
                padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                  RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(2),
                  ),
                )),
            onPressed: () {
              _confirm();
            },
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
          decoration: BoxDecoration(
            border: Border.all(color: itemBorderColor),
            borderRadius: BorderRadius.circular(2),
          ),
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
            Container(
                width: 82,
                height: 82, // 设置背景
                decoration: BoxDecoration(
                  color: Colors.blue,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Obx(() {
                  if (_iconPath.value.isEmpty) {
                    return Container();
                  } else {
                    return Image(
                      image: FileImage(File(_iconPath.value)),
                      height: 82,
                      width: 82,
                      fit: BoxFit.cover,
                    );
                  }
                })),
            const SizedBox(width: 10),
            Obx(() {
              return Row(children: [
                InkWell(
                    onTap: () async {
                      String path = await fileUtils.saveImage(150) ?? "";
                      if (path.isNotEmpty) {
                        _iconPath.value = path;
                      }
                    },
                    child: const Text('上传图标', style: TextStyle(fontSize: 14, color: Colors.blue))),
                const SizedBox(width: 10),
                if (_iconPath.value.isNotEmpty)
                  InkWell(
                      onTap: () {
                        _iconPath.value = "";
                      },
                      child: const Text('恢复默认', style: TextStyle(fontSize: 14, color: Colors.blue))),
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
          decoration: BoxDecoration(
            border: Border.all(color: itemBorderColor),
            borderRadius: BorderRadius.circular(2),
          ),
          child: Expanded(
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
          ))
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
          onPressed: () {
            Get.back();
          },
        )
      ]),
    );
  }

  void _showAlertDialog() {
    Get.dialog(Center(
        child: Container(
      width: 200,
      height: 100,
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.all(Radius.circular(6)),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Text("必填项不能为空"),
          const SizedBox(height: 10),
          TextButton(
              style: ButtonStyle(
                  padding: WidgetStateProperty.all(const EdgeInsets.fromLTRB(30, 5, 30, 5)),
                  backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
                  shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                    RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(2),
                    ),
                  )),
              onPressed: () {
                Get.back();
              },
              child: const Text("确定", style: TextStyle(color: Colors.white, fontSize: 14)))
        ],
      ),
    )));
  }
}
