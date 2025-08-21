import 'dart:async';
import 'dart:convert';
import 'dart:io';

import '../../models/mcp/mcp_server_status.dart';
import '../../models/mcp/mcp_tool_info.dart';
import '../../utils/log_util.dart';
import 'package:mcp_dart/mcp_dart.dart' as mcp_dart;

class McpService {
  static final McpService _instance = McpService._internal();
  
  factory McpService() {
    return _instance;
  }

  McpService._internal();

  // 添加dispose方法用于资源清理
  void dispose() {
    // 在这里添加资源清理代码
  }

  static const int connectionTimeoutSeconds = 30;

  static const int maxConcurrentConnections = 5;

  static const String _logPrefix = '[McpService]';
  
  /// 检查MCP配置中的所有服务器
  /// 
  /// [configContent] 配置内容字符串（JSON格式）
  /// 返回所有服务器的状态列表
  Future<List<McpServerStatus>> checkServers(String configContent) async {
    try {
      Log.i('$_logPrefix 开始检查MCP服务器');

      final config = _parseConfig(configContent);
      final mcpServers = config['mcpServers'] as Map<String, dynamic>;
      
      if (mcpServers.isEmpty) {
        Log.i('$_logPrefix 配置中没有定义任何MCP服务器');
        return [];
      }
      
      Log.i('$_logPrefix 发现 ${mcpServers.length} 个MCP服务器配置');

      final checkTasks = <Future<McpServerStatus>>[];
      for (final entry in mcpServers.entries) {
        final serverId = entry.key;
        final serverConfig = entry.value as Map<String, dynamic>;
        
        checkTasks.add(_checkSingleServerInternal(serverId, serverConfig));
      }
      
      final results = <McpServerStatus>[];
      
      for (int i = 0; i < checkTasks.length; i += maxConcurrentConnections) {
        final batchEnd = (i + maxConcurrentConnections).clamp(0, checkTasks.length);
        final batch = checkTasks.sublist(i, batchEnd);
        
        Log.i('$_logPrefix 处理第 ${(i ~/ maxConcurrentConnections) + 1} 批，包含 ${batch.length} 个服务器');
        
        try {
          final batchResults = await Future.wait(
            batch,
            eagerError: false,
          );
          results.addAll(batchResults);
        } catch (e) {
          Log.e('$_logPrefix 批次处理时发生错误: $e');
        }
      }
      
      final availableCount = results.where((r) => r.isAvailable).length;
      final totalCount = results.length;
      
      Log.i('$_logPrefix MCP服务器检查完成: $availableCount/$totalCount 个服务器可用');
      
      results.sort((a, b) => a.serverId.compareTo(b.serverId));
      
      return results;
      
    } catch (e, stackTrace) {
      Log.e('$_logPrefix 检查MCP服务器时发生错误', e, stackTrace);
      rethrow;
    }
  }
  
  /// 检查单个服务器
  /// 
  /// [configContent] 包含单个服务器配置的完整JSON字符串
  /// JSON格式应为: {"mcpServers": {"serverId": {"command": "...", "args": [...]}}}
  /// 返回服务器状态
  Future<McpServerStatus> checkServer(String configContent) async {
    try {
      Log.i('$_logPrefix 开始检查单个MCP服务器');
      
      final config = _parseConfig(configContent);
      final mcpServers = config['mcpServers'] as Map<String, dynamic>;
      
      if (mcpServers.isEmpty) {
        throw const FormatException('配置中没有定义任何MCP服务器');
      }
      
      if (mcpServers.length > 1) {
        Log.w('$_logPrefix 警告: 配置包含多个服务器，只检查第一个');
      }
      
      final entry = mcpServers.entries.first;
      final serverId = entry.key;
      final serverConfig = entry.value as Map<String, dynamic>;
      
      Log.i('$_logPrefix 检查服务器: $serverId');
      
      return await _checkSingleServerInternal(serverId, serverConfig);
      
    } catch (e, stackTrace) {
      Log.e('$_logPrefix 检查单个MCP服务器时发生错误', e, stackTrace);
      rethrow;
    }
  }
  
  /// 解析MCP配置内容
  /// 
  /// [configContent] 配置内容字符串
  /// 返回解析后的配置Map
  Map<String, dynamic> _parseConfig(String configContent) {
    try {
      if (configContent.trim().isEmpty) {
        throw const FormatException('配置内容为空');
      }
      
      final Map<String, dynamic> config;
      try {
        config = jsonDecode(configContent) as Map<String, dynamic>;
      } catch (e) {
        throw FormatException('配置JSON格式无效: $e');
      }
      
      if (!config.containsKey('mcpServers')) {
        throw const FormatException('配置缺少 mcpServers 字段');
      }
      
      final mcpServers = config['mcpServers'];
      if (mcpServers is! Map<String, dynamic>) {
        throw const FormatException('mcpServers 字段格式无效，应为对象');
      }
      
      for (final entry in mcpServers.entries) {
        final serverId = entry.key;
        final serverConfig = entry.value;
        
        if (serverConfig is! Map<String, dynamic>) {
          throw FormatException('服务器配置 $serverId 格式无效');
        }
        
        if (!serverConfig.containsKey('command')) {
          throw FormatException('服务器配置 $serverId 缺少 command 字段');
        }
        
        if (serverConfig['command'] is! String) {
          throw FormatException('服务器配置 $serverId 的 command 字段必须为字符串');
        }
        
        if (serverConfig.containsKey('args') && 
            serverConfig['args'] is! List) {
          throw FormatException('服务器配置 $serverId 的 args 字段必须为数组');
        }
      }
      
      return config;
    } catch (e) {
      rethrow;
    }
  }
  
  /// 检查单个服务器（内部方法）
  /// 
  /// [serverId] 服务器ID
  /// [serverConfig] 服务器配置
  /// 返回服务器状态
  Future<McpServerStatus> _checkSingleServerInternal(
    String serverId, 
    Map<String, dynamic> serverConfig,
  ) async {
    try {
      final command = serverConfig['command'] as String;
      final args = (serverConfig['args'] as List<dynamic>?)
          ?.map((e) => e.toString())
          .toList() ?? [];
      
      Log.i('$_logPrefix 检查服务器: $serverId, 命令: $command, 参数: $args');
      
      final result = await _connectAndGetTools(command, args);
      
      return McpServerStatus.available(
        serverId: serverId,
        serverName: result.serverName,
        tools: result.tools,
        version: result.version,
      );
      
    } on TimeoutException catch (e) {
      Log.w('$_logPrefix 服务器 $serverId 连接超时: ${e.message}');
      return McpServerStatus.unavailable(
        serverId: serverId,
        errorMessage: '连接超时: ${e.message}',
      );
    } on ProcessException catch (e) {
      Log.w('$_logPrefix 服务器 $serverId 进程启动失败: ${e.message}');
      return McpServerStatus.unavailable(
        serverId: serverId,
        errorMessage: '进程启动失败: ${e.message}',
      );
    } on FileSystemException catch (e) {
      Log.w('$_logPrefix 服务器 $serverId 文件系统错误: ${e.message}');
      return McpServerStatus.unavailable(
        serverId: serverId,
        errorMessage: '文件系统错误: ${e.message}',
      );
    } catch (e, stackTrace) {
      Log.e('$_logPrefix 服务器 $serverId 检查失败: $e', e, stackTrace);
      return McpServerStatus.unavailable(
        serverId: serverId,
        errorMessage: '连接失败: ${e.toString()}',
      );
    }
  }
  
  /// 连接服务器并获取工具列表
  /// 
  /// [command] 启动命令
  /// [args] 命令参数
  /// 返回连接结果（服务器信息和工具列表）
  Future<({String serverName, String? version, List<McpToolInfo> tools})> 
      _connectAndGetTools(String command, List<String> args) async {
    mcp_dart.StdioClientTransport? transport;
    mcp_dart.Client? client;
    
    try {
      transport = mcp_dart.StdioClientTransport(
        mcp_dart.StdioServerParameters(
          command: command,
          args: args,
          stderrMode: ProcessStartMode.normal,
        ),
      );
      
      client = mcp_dart.Client(
        mcp_dart.Implementation(name: "lite-agent-mcp-checker", version: "1.0.0"),
      );
      
      final Completer<void> errorCompleter = Completer<void>();
      transport.onerror = (error) {
        if (!errorCompleter.isCompleted) {
          errorCompleter.completeError(error);
        }
      };
      
      Timer? timeoutTimer;
      timeoutTimer = Timer(const Duration(seconds: connectionTimeoutSeconds), () {
        if (!errorCompleter.isCompleted) {
          errorCompleter.completeError(
            TimeoutException('连接超时', const Duration(seconds: connectionTimeoutSeconds)),
          );
        }
      });
      
      try {
        await Future.any([
          client.connect(transport),
          errorCompleter.future,
        ]);
        
        timeoutTimer.cancel();
        
        final serverVersion = client.getServerVersion();
        final serverName = serverVersion?.name ?? 'Unknown Server';
        final version = serverVersion?.version;
        
        final toolsResult = await client.listTools();
        final tools = toolsResult.tools.map((tool) => 
          McpToolInfo(
            name: tool.name,
            description: tool.description ?? '',
          ),
        ).toList();
        
        return (
          serverName: serverName,
          version: version,
          tools: tools,
        );
        
      } finally {
        timeoutTimer?.cancel();
      }
      
    } catch (e) {
      rethrow;
    } finally {
      try {
        await client?.close();
      } catch (e) {
        Log.e('$_logPrefix 关闭客户端时发生错误: $e');
      }
    }
  }
} 