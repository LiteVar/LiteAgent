extension IntExtension on int {
  String toShortForm() {
    if (this < 1000) {
      return '$this';
    } else if (this < 1000000) {
      double value = this / 1000;
      return _formatValue(value, 'k');
    } else {
      double value = this / 1000000;
      return _formatValue(value, 'm');
    }
  }

  String _formatValue(double value, String unit) {
    if (value % 1 == 0) {
      return '${value.toInt()}$unit';
    } else {
      return '${value.toStringAsFixed(1)}$unit';
    }
  }
}
