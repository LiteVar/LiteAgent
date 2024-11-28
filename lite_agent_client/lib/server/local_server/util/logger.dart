import 'dart:io';
import 'package:logging/logging.dart';
import '../config.dart';

enum LogModule { http, ws, agent }

AgentLogger logger = AgentLogger(config.log.level);

class AgentLogger {
  static final AgentLogger _singleton = AgentLogger._internal();
  static Level _level = Level.INFO;

  factory AgentLogger(Level level) {
    _level = level;
    return _singleton;
  }

  Logger? _logger;
  late File _logFile;

  AgentLogger._internal() {
    _logger = Logger('LiteAgentCoreServerLogger');
    Logger.root.level = _level;
    /*_logFile = File(
        '${Directory.current.path}${Platform.pathSeparator}log${Platform.pathSeparator}agent.log');
    _logFile.createSync(recursive: true);*/

    Logger.root.onRecord.listen((record) {
      final message =
          '${record.level.name}: ${record.time}: PID $pid: ${record.message}';
      print(message);
      //_logFile.writeAsStringSync('$message\n', mode: FileMode.append);
    });
  }

  void log(LogModule module, String message,
      {String detail = "", Level level = Level.INFO}) {
    _logger?.log(level, "[${module.name.toUpperCase()}] $message - $detail");
  }
}
