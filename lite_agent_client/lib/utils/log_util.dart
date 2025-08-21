import 'package:logger/logger.dart';
import 'package:path_provider/path_provider.dart';
import 'dart:io';

class CustomLogPrinter extends LogPrinter {
  @override
  List<String> log(LogEvent event) {
    final time = _formatTime(DateTime.now());
    final level = _formatLevel(event.level);
    final message = event.message.toString();
    
    return ['$time $level $message'];
  }

  String _formatTime(DateTime time) {
    return '[${time.year.toString().padLeft(4, '0')}-'
           '${time.month.toString().padLeft(2, '0')}-'
           '${time.day.toString().padLeft(2, '0')} '
           '${time.hour.toString().padLeft(2, '0')}:'
           '${time.minute.toString().padLeft(2, '0')}:'
           '${time.second.toString().padLeft(2, '0')}]';
  }

  String _formatLevel(Level level) {
    switch (level) {
      case Level.debug:
        return '[DEBUG]';
      case Level.info:
        return '[INFO]';
      case Level.warning:
        return '[WARN]';
      case Level.error:
        return '[ERROR]';
      default:
        return '[${level.toString().toUpperCase()}]';
    }
  }
}

class Log {
  static Logger? _logger;

  static Future<void> init() async {
    final appDir = await getApplicationSupportDirectory();
    final configDir = Directory('${appDir.path}${Platform.pathSeparator}log');
    if (!configDir.existsSync()) {
      configDir.createSync(recursive: true);
    }
    final file = File('${configDir.path}${Platform.pathSeparator}app.log');

    _logger = Logger(
      printer: CustomLogPrinter(),
      output: MultiOutput([ConsoleOutput(), FileOutput(file: file)]),
    );
  }

  /// Log a message at level [Level.debug].
  static void d(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    _logger?.log(Level.debug, message, error: error, stackTrace: stackTrace);
  }

  /// Log a message at level [Level.info].
  static void i(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    _logger?.log(Level.info, message, error: error, stackTrace: stackTrace);
  }

  /// Log a message at level [Level.warning].
  static void w(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    _logger?.log(Level.warning, message, error: error, stackTrace: stackTrace);
  }

  /// Log a message at level [Level.error].
  static void e(dynamic message, [dynamic error, StackTrace? stackTrace]) {
    _logger?.log(Level.error, message, error: error, stackTrace: stackTrace);
  }
}
