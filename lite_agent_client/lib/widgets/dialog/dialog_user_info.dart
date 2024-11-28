import 'package:flutter/material.dart';

class UserInfoDialog extends StatelessWidget {
  final String name;
  final String iconPath;

  const UserInfoDialog({
    super.key,
    required this.name,
    required this.iconPath,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
        child: Container(
            width: 203,
            height: 125,
            padding: const EdgeInsets.only(top: 18),
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.all(Radius.circular(6)),
            ),
            child: Column(
              children: [
                Container(width: 40, height: 40, color: Colors.grey),
                const SizedBox(height: 22),
                Text(name, style: const TextStyle(color: Colors.black, fontSize: 14))
              ],
            )));
  }
}
