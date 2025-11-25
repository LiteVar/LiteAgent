import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:lite_agent_client/utils/log_util.dart';

class HiveMigrationUtil {
  static const String _boxesFolderName = 'boxes';
  
  static Future<Directory> getNewDirectory() async {
    final appDir = await getApplicationSupportDirectory();
    final newDir = Directory('${appDir.path}${Platform.pathSeparator}$_boxesFolderName');
    return newDir;
  }
  
  static Future<Directory> getOldDirectory() async {
    final documentsDir = await getApplicationDocumentsDirectory();
    return documentsDir;
  }
  
  static bool hasHiveData(Directory directory) {
    if (!directory.existsSync()) {
      return false;
    }
    final hiveFiles = directory.listSync()
        .where((entity) => entity is File && (entity.path.endsWith('.hive') || entity.path.endsWith('.lock')))
        .toList();
    
    return hiveFiles.isNotEmpty;
  }
  
  static List<File> getHiveFiles(Directory directory) {
    if (!directory.existsSync()) {
      return [];
    }
    return directory.listSync()
        .where((entity) => entity is File && (entity.path.endsWith('.hive') || entity.path.endsWith('.lock')))
        .cast<File>()
        .toList();
  }
  
  static Future<void> copyFile(File sourceFile, Directory targetDir) async {
    if (!targetDir.existsSync()) {
      targetDir.createSync(recursive: true);
    }
    
    final targetFile = File('${targetDir.path}${Platform.pathSeparator}${sourceFile.uri.pathSegments.last}');
    await sourceFile.copy(targetFile.path);
    Log.i('文件已复制: ${sourceFile.path} -> ${targetFile.path}');
  }
  
  static Future<String> migrateHiveData() async {
    try {
      final newDir = await getNewDirectory();
      final oldDir = await getOldDirectory();
      
      final hasNewData = hasHiveData(newDir);
      final hasOldData = hasHiveData(oldDir);
      Log.i('新目录数据检查: $hasNewData, 旧目录数据检查: $hasOldData');
      
      if (hasNewData) {
        Log.i('使用新目录: ${newDir.path}');
        return newDir.path;
      } else if (hasOldData) {
        Log.i('开始从旧目录迁移数据到新目录');
        
        final hiveFiles = getHiveFiles(oldDir);
        for (final file in hiveFiles) {
          await copyFile(file, newDir);
        }
        
        Log.i('数据迁移完成，使用新目录: ${newDir.path}');

        Future.delayed(const Duration(seconds: 10), () {
          HiveMigrationUtil.safeCleanupOldHiveFiles();
        });

        return newDir.path;
      } else {
        Log.i('没有现有数据，使用新目录: ${newDir.path}');
        return newDir.path;
      }
    } catch (e) {
      Log.e('Hive迁移错误', e);
      final fallbackDir = await getOldDirectory();
      Log.w('迁移失败，使用默认目录: ${fallbackDir.path}');
      return fallbackDir.path;
    }
  }
  
  static Future<void> safeCleanupOldHiveFiles() async {
    try {
      final newDir = await getNewDirectory();
      final oldDir = await getOldDirectory();
      if (!hasHiveData(newDir)) {
        Log.w('新目录没有数据，跳过清理');
        return;
      }
      if (!hasHiveData(oldDir)) {
        Log.i('旧目录没有数据，无需清理');
        return;
      }
      final newFiles = getHiveFiles(newDir);
      final oldFiles = getHiveFiles(oldDir);
      
      if (newFiles.length < oldFiles.length) {
        Log.w('新目录文件数量少于旧目录，跳过清理以确保数据安全');
        return;
      }
      for (final file in oldFiles) {
        await file.delete();
        Log.i('已删除旧文件: ${file.path}');
      }
      Log.i('旧目录安全清理完成');
    } catch (e) {
      Log.e('Hive迁移清理错误', e);
    }
  }
  
  static Future<void> forceCleanupOldHiveFiles() async {
    try {
      final oldDir = await getOldDirectory();
      final hiveFiles = getHiveFiles(oldDir);
      
      for (final file in hiveFiles) {
        await file.delete();
        Log.i('已强制删除旧文件: ${file.path}');
      }
      
      Log.i('旧目录强制清理完成');
    } catch (e) {
      Log.e('Hive迁移清理错误', e);
    }
  }
}
