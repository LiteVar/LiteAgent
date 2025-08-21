import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:image/image.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import 'package:path_provider/path_provider.dart';

final fileUtils = FileUtils();

class FileUtils {
  Future<String?> selectImgFile() async {
    var result = await FilePicker.platform.pickFiles(type: FileType.image);
    if (result != null && result.files.single.path != null) {
      String imgPath = result.files.single.path ?? "";
      return imgPath;
    }
    return null;
  }

  Future<String> _getImgDirectory() async {
    final appDirectory = await getApplicationSupportDirectory();
    final imgDirectoryPath = '${appDirectory.path}${Platform.pathSeparator}img';
    try {
      if (!Directory(imgDirectoryPath).existsSync()) {
        Directory(imgDirectoryPath).create();
        Log.i('文件夹创建成功: $imgDirectoryPath');
      }
    } catch (e) {
      Log.e('创建文件夹时出错: $e');
    }
    return imgDirectoryPath;
  }

  Future<String?> processImage(File originalFile) async {
    try {
      // 1. 读取图片
      final bytes = await originalFile.readAsBytes();
      Image image = decodeImage(bytes)!;

      // 2. 缩放
      image = _resizeImage(image, 1024);

      // 3. 裁剪为正方形
      image = _cropSquare(image);

      // 4. 压缩并保存
      File? processedFile = await _compressAndSave(image);
      Log.i('文件已保存至: ${processedFile.path}');
      return processedFile.path;
    } catch (e) {
      Log.e('图片处理失败: $e');
    }
    return null;
  }

  Image _resizeImage(Image image, int maxSize) {
    final aspectRatio = image.width / image.height;
    return aspectRatio > 1 ? copyResize(image, width: maxSize) : copyResize(image, height: maxSize);
  }

  Image _cropSquare(Image image) {
    final size = image.width > image.height ? image.height : image.width;
    final x = (image.width - size) ~/ 2;
    final y = (image.height - size) ~/ 2;
    return copyCrop(image, x: x, y: y, width: size, height: size);
  }

  Future<File> _compressAndSave(Image image) async {
    List<int> bytes;
    String extension = 'jpg';

    // 优先尝试JPEG压缩
    int quality = 90;
    do {
      bytes = encodeJpg(image, quality: quality);
      quality -= 10;
    } while (bytes.length > 500 * 1024 && quality > 10);

    // 如果JPEG不满足则尝试PNG
    if (bytes.length > 500 * 1024) {
      bytes = encodePng(image);
      extension = 'png';
      if (bytes.length > 500 * 1024) throw Exception('无法压缩到500KB以内');
    }

    // 保存文件
    var imgDirectoryPath = await _getImgDirectory();
    var filename = 'processed_${DateTime.now().millisecondsSinceEpoch}.$extension';
    final file = File('$imgDirectoryPath${Platform.pathSeparator}$filename');
    await file.writeAsBytes(bytes);
    return file;
  }
}
