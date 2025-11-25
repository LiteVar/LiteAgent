import 'dart:io';
import 'package:archive/archive.dart';
import 'package:file_picker/file_picker.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:path_provider/path_provider.dart';
import 'package:lite_agent_client/utils/log_util.dart';

/// 文件处理服务
class FileService extends GetxService {
  // 选中的文件
  final selectedFile = Rx<File?>(null);

  // 上传的文件名
  final uploadedFileName = RxString('');

  // 是否正在拖拽文件
  final isDragging = false.obs;

  // 临时目录
  Directory? _tempDir;

  /// 重置文件服务状态
  void reset() {
    selectedFile.value = null;
    uploadedFileName.value = '';
    isDragging.value = false;
    cleanupTempDir();
  }

  /// 选择文件
  Future<void> selectFile() async {
    try {
      FilePickerResult? result = await FilePicker.platform.pickFiles(type: FileType.custom, allowedExtensions: ['agent']);

      if (result != null && result.files.isNotEmpty) {
        final file = File(result.files.first.path!);
        await _handleFileSelection(file);
      }
    } catch (e) {
      Log.e('选择文件失败: $e');
      _showErrorDialog('选择文件失败: ${e.toString()}');
    }
  }

  /// 处理拖拽文件
  Future<void> handleDragFile(List<File> files) async {
    if (files.isEmpty) return;

    final file = files.first;
    if (!file.path.toLowerCase().endsWith('.agent')) {
      _showErrorDialog('请选择.agent格式的文件');
      return;
    }

    await _handleFileSelection(file);
  }

  /// 处理文件选择
  Future<void> _handleFileSelection(File file) async {
    selectedFile.value = file;
    uploadedFileName.value = file.path.replaceAll('\\', '/').split('/').last;
    isDragging.value = false;
  }

  /// 更换文件
  Future<void> changeFile() async {
    await selectFile();
  }

  /// 解压文件到临时目录
  Future<Directory?> unpackZipToTemp(File file) async {
    try {
      Log.d('开始解压文件: ${file.path}');
      final bytes = await file.readAsBytes();
      final archive = ZipDecoder().decodeBytes(bytes);
      Log.d('ZIP文件包含 ${archive.length} 个文件/目录');

      final tempDir = await getTemporaryDirectory();
      final importDir = Directory('${tempDir.path}/agent_import_${DateTime.now().microsecondsSinceEpoch}');
      Log.d('创建临时目录: ${importDir.path}');

      if (!importDir.existsSync()) {
        importDir.createSync(recursive: true);
      }

      for (final file in archive) {
        final filename = file.name.replaceAll('\\', '/');
        Log.d('解压文件: $filename');
        if (file.isFile) {
          final filePath = '${importDir.path}/$filename';
          final fileDir = Directory(filePath.substring(0, filePath.lastIndexOf('/')));
          if (!fileDir.existsSync()) {
            fileDir.createSync(recursive: true);
          }
          final outFile = File(filePath);
          outFile.writeAsBytesSync(file.content as List<int>);
          Log.d('写入文件: $filePath');
        }
      }

      _tempDir = importDir;
      Log.d('解压完成，临时目录: ${importDir.path}');
      return importDir;
    } catch (e) {
      Log.e('解压文件失败: $e');
      return null;
    }
  }

  /// 获取临时目录
  Directory? get tempDir => _tempDir;

  /// 清理临时目录
  void cleanupTempDir() {
    if (_tempDir != null && _tempDir!.existsSync()) {
      try {
        _tempDir!.deleteSync(recursive: true);
      } catch (e) {
        Log.e('清理临时目录失败: $e');
      }
      _tempDir = null;
    }
  }

  /// 显示错误对话框
  void _showErrorDialog(String message) {
    AlarmUtil.showAlertDialog(message);
  }

  @override
  void onClose() {
    cleanupTempDir();
    super.onClose();
  }
}
