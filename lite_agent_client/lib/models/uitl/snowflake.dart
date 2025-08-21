/// snowflake include the mainly logic
class Snowflake {
  static const int _twepoch = 1546272000000;

  /// 机器id所占的位数
  static const int _workerIdBits = 5;

  /// 数据标识id所占的位数
  static const int _datacenterIdBits = 5;

  /// 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
  static const int _maxWorkerId = -1 ^ (-1 << _workerIdBits);

  /// 支持的最大数据标识id，结果是31
  static const int _maxDatacenterId = -1 ^ (-1 << _datacenterIdBits);

  /// 序列在id中占的位数
  static const int _sequenceBits = 12;

  /// 机器ID向左移12位
  static const int _workerIdShift = _sequenceBits;

  /// 数据标识id向左移17位(12+5)
  static const int _datacenterIdShift = _sequenceBits + _workerIdBits;

  /// 时间截向左移22位(5+5+12)
  static const int _timestampeftShift =
      _sequenceBits + _workerIdBits + _datacenterIdBits;

  /// 生成序列的掩码，这里为4194303
  static const int _sequenceMask = -1 ^ (-1 << _sequenceBits);

  /// 工作机器ID(0~31)
  late final int _workerId;

  /// 数据中心ID(0~31)
  late final int _datacenterId;

  /// 秒内序列(0~4194303)
  int _sequence = 0;

  /// 上次生成ID的时间截
  int _lastTimestamp = -1;

  Snowflake(this._workerId, this._datacenterId) {
    if (_workerId > _maxWorkerId || _workerId < 0) {
      throw Exception(
          "worker Id can't be greater than $_maxWorkerId or less than 0");
    }
    if (_datacenterId > _maxDatacenterId || _datacenterId < 0) {
      throw Exception(
          "datacenter Id can't be greater than $_maxDatacenterId or less than 0");
    }
  }

  int getId() {
    int timestamp = currentTimeStamp;

    // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
    if (timestamp < _lastTimestamp) {
      throw Exception(
          "Clock moved backwards.  Refusing to generate id for ${_lastTimestamp - timestamp} milliseconds");
    }

    // 如果是同一时间生成的，则进行秒内序列
    if (_lastTimestamp == timestamp) {
      _sequence = (_sequence + 1) & _sequenceMask;
      // 秒内序列溢出
      if (_sequence == 0) {
        // 阻塞到下一个秒,获得新的时间戳
        timestamp = untilNextMillis(_lastTimestamp);
      }
    }
    // 时间戳改变，秒内序列重置
    else {
      _sequence = 0;
    }

    // 上次生成ID的时间截
    _lastTimestamp = timestamp;

    // 移位并通过或运算拼到一起组成64位的ID
    return ((timestamp - _twepoch) << _timestampeftShift) |
    (_datacenterId << _datacenterIdShift) |
    (_workerId << _workerIdShift) |
    _sequence;
  }

  int untilNextMillis(int lastTimestamp) {
    int timestamp = currentTimeStamp;
    while (timestamp <= lastTimestamp) {
      timestamp = currentTimeStamp;
    }
    return timestamp;
  }

  /// 返回以秒为单位的当前时间
  /// @return 当前时间(秒)
  int get currentTimeStamp {
    return DateTime.now().millisecondsSinceEpoch;
  }
}
