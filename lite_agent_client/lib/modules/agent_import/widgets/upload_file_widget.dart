import 'dart:io';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:desktop_drop/desktop_drop.dart';
import '../logic.dart';

/// 上传文件组件
class UploadFileWidget extends StatelessWidget {
  const UploadFileWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<AgentImportLogic>();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('请选择要导入的智能体文件,上传到此处', style: TextStyle(fontSize: 16, color: Color(0xFF333333))),
        const SizedBox(height: 24),
        Obx(() => _buildUploadArea(logic)),
      ],
    );
  }

  /// 构建上传区域
  Widget _buildUploadArea(AgentImportLogic logic) {
    return DropTarget(
      onDragDone: (detail) {
        final files = detail.files.map((xfile) => File(xfile.path)).toList();
        logic.handleDragFile(files);
      },
      onDragEntered: (detail) => logic.fileService.isDragging.value = true,
      onDragExited: (detail) => logic.fileService.isDragging.value = false,
      child: Container(
        height: 120,
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: logic.fileService.isDragging.value ? const Color(0xFFf0f8ff) : const Color(0xFFfafafa),
          border: Border.all(
            color: logic.fileService.isDragging.value ? const Color(0xFF2a82f5) : const Color(0xFFd9d9d9),
            style: BorderStyle.solid,
          ),
          borderRadius: BorderRadius.circular(8),
        ),
        child: _buildContent(logic),
      ),
    );
  }

  /// 构建内容区域
  Widget _buildContent(AgentImportLogic logic) {
    // 拖拽状态显示
    if (logic.fileService.isDragging.value) {
      return _buildDragOverlay();
    }

    // 已选择文件状态
    if (logic.fileService.selectedFile.value != null) {
      return _buildFileSelectedContent(logic);
    }

    // 未选择文件状态
    return _buildFileUploadContent(logic);
  }

  /// 构建拖拽覆盖层
  Widget _buildDragOverlay() {
    return const Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(Icons.cloud_upload_outlined, size: 32, color: Color(0xFF2a82f5)),
        SizedBox(height: 12),
        Text('松开鼠标完成上传', style: TextStyle(fontSize: 14, color: Color(0xFF2a82f5))),
      ],
    );
  }

  /// 构建文件已选择内容
  Widget _buildFileSelectedContent(AgentImportLogic logic) {
    return Row(
      children: [
        const Icon(Icons.insert_drive_file_outlined, size: 24, color: Color(0xFF999999)),
        const SizedBox(width: 12),
        Expanded(
          child: Text(logic.fileService.uploadedFileName.value, style: const TextStyle(fontSize: 14, color: Color(0xFF333333))),
        ),
        TextButton(
          onPressed: () => logic.changeFile(),
          style: TextButton.styleFrom(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
            minimumSize: Size.zero,
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
          child: const Text('更换文件', style: TextStyle(fontSize: 14, color: Color(0xFF2a82f5))),
        ),
      ],
    );
  }

  /// 构建文件上传内容
  Widget _buildFileUploadContent(AgentImportLogic logic) {
    return InkWell(
      onTap: () => logic.selectFile(),
      child: const Column(
        mainAxisAlignment: MainAxisAlignment.center,
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.insert_drive_file_outlined, size: 28, color: Color(0xFF999999)),
          SizedBox(height: 6),
          Text('拖拽文件或者点击此区域进行上传', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
          SizedBox(height: 1),
          Text('支持上传文档格式: .agent格式文档', style: TextStyle(fontSize: 12, color: Color(0xFF999999))),
        ],
      ),
    );
  }
}
