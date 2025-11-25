import 'package:dropdown_button2/dropdown_button2.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'common_widget.dart';

/// 通用下拉选择框组件
/// 提供统一的下拉选择框样式和交互
class CommonDropdown<T> extends StatelessWidget {
  final T? selectedValue;
  final List<DropdownItem<T>> items;
  final Function(T?) onChanged;
  final String placeholder;
  final double width;
  final double height;
  final bool isEnabled;
  final String? displayText;
  final EdgeInsets? margin;
  final EdgeInsets? padding;
  final Color? backgroundColor;
  final Color? textColor;
  final Color? disabledTextColor;
  final double fontSize;
  final String? iconPath;
  final double iconSize;
  final Color? iconColor;
  final double? dropdownWidth;
  final Offset? dropdownOffset;
  final EdgeInsets? dropdownPadding;
  final Color? dropdownBackgroundColor;
  final double? menuItemHeight;
  final Color? menuItemTextColor;
  final double? menuItemFontSize;

  const CommonDropdown({
    super.key,
    required this.selectedValue,
    required this.items,
    required this.onChanged,
    required this.placeholder,
    this.width = 200,
    this.height = 32,
    this.isEnabled = true,
    this.displayText,
    this.margin,
    this.padding,
    this.backgroundColor,
    this.textColor,
    this.disabledTextColor,
    this.fontSize = 12,
    this.iconPath,
    this.iconSize = 12,
    this.iconColor,
    this.dropdownWidth,
    this.dropdownOffset,
    this.dropdownPadding,
    this.dropdownBackgroundColor,
    this.menuItemHeight,
    this.menuItemTextColor,
    this.menuItemFontSize,
  });

  @override
  Widget build(BuildContext context) {
    return DropdownButtonHideUnderline(
      child: DropdownButton2<T>(
        customButton: Container(
          width: width,
          height: height,
          margin: margin ?? const EdgeInsets.only(top: 5),
          padding: padding ?? const EdgeInsets.symmetric(horizontal: 12),
          decoration: BoxDecoration(
            color: backgroundColor ?? const Color(0xfff5f5f5),
            borderRadius: const BorderRadius.all(Radius.circular(4)),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  _getDisplayText(),
                  style: TextStyle(
                      fontSize: fontSize, color: isEnabled ? (textColor ?? const Color(0xff333333)) : (disabledTextColor ?? Colors.grey)),
                ),
              ),
              if (iconPath != null)
                buildAssetImage(iconPath!, iconSize, iconColor ?? const Color(0xff333333))
              else
                buildAssetImage("icon_down.png", iconSize, iconColor ?? const Color(0xff333333)),
            ],
          ),
        ),
        dropdownStyleData: DropdownStyleData(
          width: dropdownWidth ?? width,
          offset: dropdownOffset ?? const Offset(-0, -8),
          padding: dropdownPadding ?? const EdgeInsets.symmetric(vertical: 0),
          decoration: BoxDecoration(color: dropdownBackgroundColor ?? Colors.white),
        ),
        menuItemStyleData: MenuItemStyleData(height: menuItemHeight ?? 40),
        items: items
            .map((item) => DropdownMenuItem<T>(
                  value: item.value,
                  child: Text(
                    item.text,
                    style: TextStyle(fontSize: menuItemFontSize ?? 13, color: menuItemTextColor ?? const Color(0xff333333)),
                  ),
                ))
            .toList(),
        value: selectedValue,
        onChanged: isEnabled ? onChanged : null,
      ),
    );
  }

  String _getDisplayText() {
    if (displayText != null) {
      return displayText!;
    }

    if (selectedValue != null) {
      final selectedItem = items.firstWhereOrNull((item) => item.value == selectedValue);
      if (selectedItem != null) {
        return selectedItem.text;
      }
    }

    return placeholder;
  }
}

/// 下拉选择框项目数据类
class DropdownItem<T> {
  final T value;
  final String text;

  const DropdownItem({required this.value, required this.text});
}

/// 简化版通用下拉选择框
/// 使用默认样式，减少参数配置
class SimpleDropdown<T> extends StatelessWidget {
  final T? selectedValue;
  final List<DropdownItem<T>> items;
  final Function(T?) onChanged;
  final String placeholder;
  final double width;
  final double height;
  final bool isEnabled;
  final String? displayText;

  const SimpleDropdown({
    super.key,
    required this.selectedValue,
    required this.items,
    required this.onChanged,
    required this.placeholder,
    this.width = 200,
    this.height = 32,
    this.isEnabled = true,
    this.displayText,
  });

  @override
  Widget build(BuildContext context) {
    return CommonDropdown<T>(
      selectedValue: selectedValue,
      items: items,
      onChanged: onChanged,
      placeholder: placeholder,
      width: width,
      height: height,
      isEnabled: isEnabled,
      displayText: displayText,
    );
  }
}
