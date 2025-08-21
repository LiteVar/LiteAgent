import 'package:flutter/cupertino.dart';

class VolumeWavePainter extends CustomPainter {
  final double volume;
  final Color color;
  final List<double> volumeHistory;
  final int historyIndex;

  VolumeWavePainter({required this.historyIndex, required this.volume, required this.color, required this.volumeHistory});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()..color = color;
    // 保持与波点相同的总数量（100个）
    const totalSeconds = 5;
    final itemWidth = size.width / (totalSeconds * 20); // 100个竖条（5×20）

    // 绘制时间轴背景（无声波点）
    _drawInactiveWave(canvas, size, paint);

    // 绘制有声部分（竖状条）
    _drawActiveBars(canvas, size, paint, itemWidth);
  }

  void _drawInactiveWave(Canvas canvas, Size size, Paint paint) {
    // 波点参数
    const dotCount = 100;
    final dotSpacing = size.width / dotCount;
    final dotRadius = size.height / 30;

    for (var i = 0; i < dotCount; i++) {
      final x = i * dotSpacing + dotSpacing / 2;
      final y = size.height / 2;
      canvas.drawCircle(Offset(x, y), dotRadius, paint..color = color.withOpacity(0.2));
    }
  }

  void _drawActiveBars(Canvas canvas, Size size, Paint paint, double itemWidth) {
    final effectiveLength = volumeHistory.length;

    for (var i = 0; i < effectiveLength; i++) {
      // 计算环形缓冲区中的实际索引
      final index = (historyIndex + i) % effectiveLength;
      final vol = volumeHistory[index];

      final height = size.height * vol;
      final x = i * itemWidth + itemWidth / 2;
      final y = (size.height - height) / 2;

      if (vol > 0) {
        canvas.drawRRect(
          RRect.fromRectAndRadius(Rect.fromLTWH(x, y, itemWidth * 0.2, height), const Radius.circular(1)),
          paint..color = color,
        );
      }
    }
  }

  @override
  bool shouldRepaint(covariant VolumeWavePainter old) {
    return old.historyIndex != historyIndex || old.volume != volume || old.volumeHistory != volumeHistory;
  }
}
