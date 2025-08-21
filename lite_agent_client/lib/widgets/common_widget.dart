import 'dart:io';

import 'package:flutter/material.dart';
import 'package:lite_agent_client/config/constants.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';

Widget buildAgentProfileImage(String iconPath) {
  var defaultImageWidget = LayoutBuilder(builder: (context, constraints) {
    double parentWidth = constraints.maxWidth;
    double padding = parentWidth / 4;
    double borderRadius = parentWidth / 8;
    return Container(
      padding: EdgeInsets.all(padding),
      decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(borderRadius)),
      child: buildAssetImage("icon_default_agent.png", 0, Colors.black),
    );
  });
  if (iconPath.startsWith(Constants.localFilePrefix)) {
    String path = (iconPath.split(Constants.localFilePrefix))[1];
    if (path.isEmpty) {
      return defaultImageWidget;
    }
    return Image.file(
      File(path),
      fit: BoxFit.cover,
      errorBuilder: (context, exception, stackTrace) => defaultImageWidget,
    );
  } else {
    var path = iconPath.fillPicLinkPrefixNoAsync();
    if (path.isEmpty) {
      return defaultImageWidget;
    }
    return Image.network(
      path,
      fit: BoxFit.cover,
      loadingBuilder: (context, result, progress) => progress == null ? result : defaultImageWidget,
      errorBuilder: (context, exception, stackTrace) => defaultImageWidget,
    );
  }
}

Widget buildUserProfileImage(String iconPath) {
  var path = iconPath.fillPicLinkPrefixNoAsync();
  var defaultImageWidget = Image.asset('assets/images/icon_default_user.png', fit: BoxFit.cover);
  if (path.isEmpty) {
    return defaultImageWidget;
  }
  return Image.network(
    path,
    fit: BoxFit.cover,
    loadingBuilder: (context, result, progress) => progress == null ? result : defaultImageWidget,
    errorBuilder: (context, exception, stackTrace) => defaultImageWidget,
  );
}

Widget buildAssetImage(String fileName, double size, Color? color) {
  return SizedBox(width: size, height: size, child: Image.asset("assets/images/$fileName", fit: BoxFit.contain, color: color));
}

InkWell buildCommonTextButton(String text, double height, double horizontalPadding, Function()? onTap) {
  return InkWell(
      onTap: onTap,
      child: Container(
        height: height,
        padding: EdgeInsets.symmetric(horizontal: horizontalPadding),
        decoration: BoxDecoration(border: Border.all(color: const Color(0xff999999)), borderRadius: BorderRadius.circular(4)),
        child: Center(child: Text(text, style: const TextStyle(fontSize: 14, color: Color(0xff999999)))),
      ));
}

TextButton buildCommonTextBlueButton(String text, Function()? onPressed) {
  return TextButton(
      style: ButtonStyle(
          padding: WidgetStateProperty.all(const EdgeInsets.symmetric(horizontal: 16)),
          backgroundColor: WidgetStateProperty.all(const Color(0xFF2a82f5)),
          shape: WidgetStateProperty.all<RoundedRectangleBorder>(
            RoundedRectangleBorder(borderRadius: BorderRadius.circular(4.0)),
          )),
      onPressed: onPressed,
      child: Text(text, style: const TextStyle(color: Colors.white, fontSize: 14)));
}

Widget horizontalLine() {
  return Container(height: 0.5, color: Colors.grey);
}

Widget verticalLine() {
  return Container(width: 0.5, color: Colors.grey);
}
