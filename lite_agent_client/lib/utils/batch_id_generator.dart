import 'package:lite_agent_client/utils/snowflake_util.dart';

/// 批量ID生成器，处理批量生成时可能出现的重复ID
class BatchIdGenerator {
  static BatchIdGenerator? _instance;
  
  final Set<String> _currentBatchIds = <String>{};

  // 私有构造函数
  BatchIdGenerator._();

  /// 获取单例实例
  static BatchIdGenerator get instance {
    _instance ??= BatchIdGenerator._();
    return _instance!;
  }

  /// 批量生成唯一ID列表，支持超大批量
  List<String> generateBatchIds(int count) {
    final ids = <String>[];
    _currentBatchIds.clear();
    
    // 对于超大批量，使用更智能的策略
    if (count > 1000) {
      return _generateLargeBatchIds(count);
    }
    
    for (int i = 0; i < count; i++) {
      String id;
      int attempts = 0;
      const maxAttempts = 20; // 增加重试次数
      
      do {
        id = snowFlakeUtil.getId();
        attempts++;
        
        if (attempts >= maxAttempts) {
          // 如果重试次数过多，使用时间戳+随机数作为备选
          id = _generateFallbackId();
          break;
        }
      } while (_currentBatchIds.contains(id));
      
      _currentBatchIds.add(id);
      ids.add(id);
    }
    
    return ids;
  }

  /// 超大批量ID生成策略
  List<String> _generateLargeBatchIds(int count) {
    final ids = <String>[];
    final batchSize = 1000; // 每批1000个
    
    for (int batch = 0; batch < count; batch += batchSize) {
      final currentBatchSize = (batch + batchSize > count) ? count - batch : batchSize;
      final batchIds = _generateBatchIdsOptimized(currentBatchSize);
      ids.addAll(batchIds);
      
      // 清理当前批次，为下一批做准备
      _currentBatchIds.clear();
    }
    
    return ids;
  }

  /// 优化的批量ID生成
  List<String> _generateBatchIdsOptimized(int count) {
    final ids = <String>[];
    
    for (int i = 0; i < count; i++) {
      String id;
      int attempts = 0;
      const maxAttempts = 50; // 超大批量时增加重试次数
      
      do {
        id = snowFlakeUtil.getId();
        attempts++;
        
        if (attempts >= maxAttempts) {
          // 使用改进的备选方案
          id = _generateFallbackIdOptimized();
          break;
        }
      } while (_currentBatchIds.contains(id));
      
      _currentBatchIds.add(id);
      ids.add(id);
    }
    
    return ids;
  }

  /// 生成单个唯一ID（在当前批次中唯一）
  String generateUniqueId() {
    String id;
    int attempts = 0;
    const maxAttempts = 10;
    
    do {
      id = snowFlakeUtil.getId();
      attempts++;
      
      if (attempts >= maxAttempts) {
        id = _generateFallbackId();
        break;
      }
    } while (_currentBatchIds.contains(id));
    
    _currentBatchIds.add(id);
    return id;
  }

  /// 生成备选ID（当Snowflake重试失败时使用）
  String _generateFallbackId() {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    final random = (timestamp % 1000000).toString().padLeft(6, '0');
    return '$timestamp$random';
  }

  /// 优化的备选ID生成（用于超大批量）
  String _generateFallbackIdOptimized() {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    final microsecond = DateTime.now().microsecond;
    final random = (microsecond % 1000000).toString().padLeft(6, '0');
    return '$timestamp$random';
  }

  /// 清理当前批次的ID记录
  void clearBatch() {
    _currentBatchIds.clear();
  }

  /// 获取当前批次已生成的ID数量
  int getCurrentBatchSize() {
    return _currentBatchIds.length;
  }
}
