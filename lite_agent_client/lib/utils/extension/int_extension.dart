extension IntExtension on int {
  String toShortForm() {
    if (this < 1000) {
      // 小于1000直接返回原数字
      return '$this';
    } else if (this < 1000000) {
      // 1000-999999 转换为k格式
      double value = this / 1000;
      return _formatValue(value, 'k');
    } else {
      // 1000000及以上转换为m格式
      double value = this / 1000000;
      return _formatValue(value, 'm');
    }
  }

  String _formatValue(double value, String unit) {
    if (value % 1 == 0) {
      // 如果是整数，不显示小数点
      return '${value.toInt()}$unit';
    } else {
      // 如果是小数，保留一位小数
      return '${value.toStringAsFixed(1)}$unit';
    }
  }
}
