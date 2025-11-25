import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../logic.dart';

class CreateConfigWidget extends StatefulWidget {
  const CreateConfigWidget({super.key});

  @override
  State<CreateConfigWidget> createState() => _CreateConfigWidgetState();
}

class _CreateConfigWidgetState extends State<CreateConfigWidget> {
  final ScrollController _scrollController = ScrollController();
  AgentImportLogic? _logic;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _logic = Get.find<AgentImportLogic>();
      // 监听消息变化
      ever(_logic!.importService.importProgressMessages, (_) {
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

    // 创建配置步骤不再自动开始导入，改为手动点击"开始"

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
    return const Text(
      '创建配置',
      style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500, color: Color(0xFF333333)),
    );
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
        child: Obx(() => _buildImportContent(logic)),
      ),
    );
  }

  /// 构建导入内容
  Widget _buildImportContent(AgentImportLogic logic) {
    // 创建配置页面仅展示导入阶段的信息，不与解析阶段混用
    final messages = logic.importService.importProgressMessages;
    
    if (messages.isEmpty) {
      return _buildLoadingState();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 显示进度消息
        ...messages.map((msg) => _buildMessageItem(msg)),
        
        // 显示导入中的加载动画
        if (logic.importService.isImporting.value) _buildImportingIndicator(),
      ],
    );
  }

  /// 构建加载状态
  Widget _buildLoadingState() {
    return const Center(
      child: Padding(
        padding: EdgeInsets.only(top: 40),
        child: Text('点击右下角“开始”按钮执行导入', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
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

  /// 构建导入指示器
  Widget _buildImportingIndicator() {
    return const Padding(
      padding: EdgeInsets.only(top: 16),
      child: Row(
        children: [
          SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
          SizedBox(width: 8),
          Text('导入中...', style: TextStyle(fontSize: 14, color: Color(0xFF666666))),
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
    } else if (message.startsWith('[注意]') || message.startsWith('[警告]')) {
      return MessageType.warning;
    } else if (message.startsWith('[错误]') || message.startsWith('[失败]')) {
      return MessageType.error;
    } else {
      return MessageType.info;
    }
  }

  // 取消自动开始逻辑
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
