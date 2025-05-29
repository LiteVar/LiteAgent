import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:image/image.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
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
        print('文件夹创建成功: $imgDirectoryPath');
      } else {
        //print('文件夹已经创建过啦');
      }
    } catch (e) {
      print('创建文件夹时出错: $e');
    }
    return imgDirectoryPath;
  }

  Future<String?> copyImgFileToImgDirectory(String originImgPath, String targetDirectory) async {
    if (Directory(targetDirectory).existsSync()) {
      try {
        final File originalImageFile = File(originImgPath);
        String pathSeparator = Platform.pathSeparator;
        final String targetPath = '$targetDirectory$pathSeparator${originalImageFile.path.split(pathSeparator).last}';
        await originalImageFile.copy(targetPath);
        print('图片已复制到: $targetPath');
        return targetPath;
      } catch (e) {
        print('图片复制出错: $e');
        return null;
      }
    }
    return null;
  }

  Future<String?> saveImage(File imageFile) async {
    File file = imageFile;
    var size = imageFile.readAsBytesSync().length / 1024;
    if (size > (1024 * 2)) {
      AlarmUtil.showAlertToast("图片大小不能超过2M");
      return null;
    }

    String targetPath = await _getImgDirectory();
    return copyImgFileToImgDirectory(file.path, targetPath);
  }

  static Future<String?> processImage(File originalFile) async {
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
      print('文件已保存至: ${processedFile.path}');
      return processedFile.path;
    } catch (e) {
      print('图片处理失败: $e');
    }
    return null;
  }

  static Image _resizeImage(Image image, int maxSize) {
    final aspectRatio = image.width / image.height;
    return aspectRatio > 1 ? copyResize(image, width: maxSize) : copyResize(image, height: maxSize);
  }

  static Image _cropSquare(Image image) {
    final size = image.width > image.height ? image.height : image.width;
    final x = (image.width - size) ~/ 2;
    final y = (image.height - size) ~/ 2;
    return copyCrop(image, x, y, size, size);
  }

  static Future<File> _compressAndSave(Image image) async {
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
    final directory = await getApplicationSupportDirectory();
    final imgDirectoryPath = '${directory.path}${Platform.pathSeparator}img';
    final filename = 'processed_${DateTime.now().millisecondsSinceEpoch}.$extension';
    return File('$imgDirectoryPath/$filename')..writeAsBytes(bytes);
  }
}
