import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/utils/log_util.dart';
import '../logic.dart';

/// 解析文件配置组件
class ParsingWidget extends StatefulWidget {
  const ParsingWidget({super.key});

  @override
  State<ParsingWidget> createState() => _ParsingWidgetState();
}

class _ParsingWidgetState extends State<ParsingWidget> {
  final ScrollController _scrollController = ScrollController();
  AgentImportLogic? _logic;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _logic = Get.find<AgentImportLogic>();
      _startParsingIfNeeded(_logic!);
      // 监听消息变化
      ever(_logic!.parsingService.parsingProgressMessages, (_) {
        _scrollToBottom();
      });
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  /// 滚动到底部（无动画）
  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollController.hasClients) {
          _scrollController.jumpTo(_scrollController.position.maxScrollExtent);
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final logic = Get.find<AgentImportLogic>();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildTitle(),
        const SizedBox(height: 24),
        Expanded(child: _buildContent(logic)),
      ],
    );
  }

  /// 构建标题
  Widget _buildTitle() {
    return const Text('解析文件配置', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500, color: Color(0xFF333333)));
  }

  /// 构建内容区域
  Widget _buildContent(AgentImportLogic logic) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFfafafa),
        border: Border.all(color: const Color(0xFFd9d9d9)),
        borderRadius: BorderRadius.circular(4),
      ),
      child: SingleChildScrollView(
        controller: _scrollController,
        child: Obx(() => _buildParsingContent(logic)),
      ),
    );
  }

  /// 构建解析内容
  Widget _buildParsingContent(AgentImportLogic logic) {
    // 检查是否有解析错误
    final hasErrors = _hasParseErrors(logic);

    if (logic.parsingService.parsingProgressMessages.isEmpty) {
      return _buildLoadingState();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 显示进度消息（解析阶段）
        ...logic.parsingService.parsingProgressMessages.map((msg) => _buildMessageItem(msg)),

        // 显示错误信息
        if (hasErrors) ..._buildErrorMessages(logic),

        // 显示解析中的加载动画
        if (_isParsing(logic)) _buildParsingIndicator(),
      ],
    );
  }

  /// 构建加载状态
  Widget _buildLoadingState() {
    return const Center(
      child: Column(
        children: [
          SizedBox(height: 40),
          CircularProgressIndicator(),
          SizedBox(height: 16),
          Text('正在准备解析...', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
        ],
      ),
    );
  }

  /// 构建消息项
  Widget _buildMessageItem(String message) {
    final messageType = _getMessageType(message);

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Icon(messageType.icon, size: 16, color: messageType.color),
          const SizedBox(width: 8),
          Expanded(
            child: Text(message, style: TextStyle(fontSize: 14, color: messageType.isError ? messageType.color : const Color(0xFF666666))),
          ),
        ],
      ),
    );
  }

  /// 构建错误消息
  List<Widget> _buildErrorMessages(AgentImportLogic logic) {
    final errors = <Widget>[];

    if (logic.parsingService.modelParseError.value != null) {
      errors.add(_buildErrorMessage('模型解析失败: ${logic.parsingService.modelParseError.value}'));
    }
    if (logic.parsingService.toolParseError.value != null) {
      errors.add(_buildErrorMessage('工具解析失败: ${logic.parsingService.toolParseError.value}'));
    }
    if (logic.parsingService.agentParseError.value != null) {
      errors.add(_buildErrorMessage('智能体解析失败: ${logic.parsingService.agentParseError.value}'));
    }
    if (logic.parsingService.knowledgeBaseParseError.value != null) {
      errors.add(_buildErrorMessage('知识库解析失败: ${logic.parsingService.knowledgeBaseParseError.value}'));
    }

    return errors;
  }

  /// 构建错误消息项
  Widget _buildErrorMessage(String message) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8, top: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          const Icon(Icons.error, size: 16, color: Color(0xFFff4d4f)),
          const SizedBox(width: 8),
          Expanded(child: Text(message, style: const TextStyle(fontSize: 14, color: Color(0xFFff4d4f))))
        ],
      ),
    );
  }

  /// 构建解析指示器
  Widget _buildParsingIndicator() {
    return const Padding(
      padding: EdgeInsets.only(top: 16),
      child: Row(
        children: [
          SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
          SizedBox(width: 8),
          Text('解析中...', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
        ],
      ),
    );
  }

  /// 获取消息类型
  MessageType _getMessageType(String message) {
    if (message.startsWith('[任务]')) {
      return MessageType.task;
    } else if (message.startsWith('[完成]')) {
      return MessageType.success;
    } else if (message.startsWith('[注意]')) {
      return MessageType.warning;
    } else if (message.startsWith('[错误]')) {
      return MessageType.error;
    } else {
      return MessageType.info;
    }
  }

  /// 检查是否有解析错误
  bool _hasParseErrors(AgentImportLogic logic) {
    return logic.parsingService.modelParseError.value != null ||
        logic.parsingService.toolParseError.value != null ||
        logic.parsingService.knowledgeBaseParseError.value != null ||
        logic.parsingService.agentParseError.value != null;
  }

  /// 检查是否正在解析
  bool _isParsing(AgentImportLogic logic) {
    return logic.parsingService.isParsingModels.value ||
        logic.parsingService.isParsingTools.value ||
        logic.parsingService.isParsingKnowledgeBases.value ||
        logic.parsingService.isParsingAgents.value;
  }

  /// 开始解析（如果需要）
  void _startParsingIfNeeded(AgentImportLogic logic) {
    // 每次进入解析页面时都重新解析
    if (!logic.parsingService.isParsingModels.value &&
        !logic.parsingService.isParsingTools.value &&
        !logic.parsingService.isParsingKnowledgeBases.value &&
        !logic.parsingService.isParsingAgents.value) {
      Log.d('ParsingWidget: 开始执行解析');
      logic.startParsing();
    } else {
      Log.d('ParsingWidget: 正在解析中，跳过重复解析');
    }
  }
}

/// 消息类型枚举
enum MessageType {
  task(Icons.info_outline, Color(0xFF2a82f5), false),
  success(Icons.check_circle_outline, Color(0xFF52c41a), false),
  warning(Icons.warning_amber_outlined, Color(0xFFfa8c16), false),
  error(Icons.error_outline, Color(0xFFff4d4f), true),
  info(Icons.circle, Color(0xFF666666), false);

  const MessageType(this.icon, this.color, this.isError);

  final IconData icon;
  final Color color;
  final bool isError;
}
