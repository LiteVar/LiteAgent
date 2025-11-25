import 'package:flutter/material.dart';

/// 通用 Radio 组件
class RadioGroupWidget {
  /// 通用 Radio 构建方法
  static Widget buildRadio<T>({
    required T value,
    required T? groupValue,
    required ValueChanged<T?> onChanged,
    required String text,
  }) {
    return Row(
      children: [
        Radio<T>(
          activeColor: Colors.blue,
          value: value,
          groupValue: groupValue,
          onChanged: onChanged,
          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
          visualDensity: const VisualDensity(horizontal: VisualDensity.minimumDensity, vertical: VisualDensity.minimumDensity),
          splashRadius: 0,
        ),
        Text(text, style: const TextStyle(fontSize: 14)),
      ],
    );
  }

  /// 通用 Radio 组构建方法
  static Widget buildRadioGroup<T>({
    required List<RadioOption<T>> options,
    required T? groupValue,
    required ValueChanged<T?> onChanged,
    double spacing = 40,
  }) {
    return Row(
      children: [
        for (int i = 0; i < options.length; i++) ...[
          buildRadio<T>(value: options[i].value, groupValue: groupValue, onChanged: onChanged, text: options[i].text),
          if (i < options.length - 1) SizedBox(width: spacing),
        ],
      ],
    );
  }
}

/// Radio 选项数据类
class RadioOption<T> {
  final T value;
  final String text;

  const RadioOption({required this.value, required this.text});
}
