import 'dart:io';
import 'dart:typed_data';
import 'package:file_picker/file_picker.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:path_provider/path_provider.dart';

final fileUtils = FileUtils();

class FileUtils {
  Future<String?> saveImage(int minSize) async {
    var result = await FilePicker.platform.pickFiles(type: FileType.image);
    if (result != null && result.files.single.path != null) {
      String imgPath = result.files.single.path ?? "";
      final file = File(imgPath);
      return await compressAndCopyImage(
          file, minSize, (await _getImgDirectory()));
    }
    return null;
  }

  Future<String> _getImgDirectory() async {
    final appDirectory = await getApplicationSupportDirectory();
    final imgDirectoryPath = '${appDirectory.path}/img';
    try {
      if (!Directory(imgDirectoryPath).existsSync()) {
        Directory(imgDirectoryPath).create();
        print('文件夹创建成功: $imgDirectoryPath');
      } else {
        print('文件夹已经创建过啦');
      }
    } catch (e) {
      print('创建文件夹时出错: $e');
    }
    return imgDirectoryPath;
  }

  Future<void> copyImgFileToImgDirectory(
      String originImgPath, String targetDirectory) async {
    if (Directory(targetDirectory).existsSync()) {
      final File originalImageFile = File(originImgPath);
      final String targetPath =
          '${targetDirectory}/${originalImageFile.path.split('/').last}';
      await originalImageFile.copy(targetPath);
      print('图片已复制到: $targetPath');
    }
  }

  Future<String?> compressAndCopyImage(
      File imageFile, int minSize, String targetPath) async {
    Uint8List? compressedBytes;
    try {
      compressedBytes = await FlutterImageCompress.compressWithFile(
        imageFile.path,
        keepExif: false,
        minHeight: minSize,
        minWidth: minSize,
      );
      if (compressedBytes != null) {
        print(
            'originSize: ${(imageFile.readAsBytesSync().lengthInBytes / 1024)}');
        print('compressSize: ${(compressedBytes.lengthInBytes / 1024)}');

        final targetDirectory = Directory(targetPath);
        if (!targetDirectory.existsSync()) {
          targetDirectory.createSync(recursive: true);
        }

        // 构建目标文件路径
        final String fileName =
            "compress_${minSize}_${imageFile.path.split('/').last}";
        final String targetFilepath = '$targetPath/$fileName';

        // 将压缩后的图片数据写入文件
        File(targetFilepath).writeAsBytesSync(compressedBytes);
        print('图片已压缩并保存到: $targetFilepath');
        return targetFilepath;
      }
    } catch (e) {
      print('图片压缩出错: $e');
    }
    return null;
  }
}
