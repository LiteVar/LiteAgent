import 'package:flutter/material.dart';
import 'package:lite_agent_client/models/dto/library.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

/// 知识库项目组件
/// 负责单个知识库项目的展示和交互
class LibraryItem extends StatelessWidget {
  final LibraryDto library;
  final bool isHovered;
  final VoidCallback onRemove;
  final VoidCallback onHoverEnter;
  final VoidCallback onHoverExit;

  const LibraryItem({
    super.key,
    required this.library,
    required this.isHovered,
    required this.onRemove,
    required this.onHoverEnter,
    required this.onHoverExit,
  });

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (event) => onHoverEnter(),
      onExit: (event) => onHoverExit(),
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(color: isHovered ? const Color(0xfff5f5f5) : Colors.transparent, borderRadius: BorderRadius.circular(8)),
        child: Row(
          children: [
            Container(
              width: 30,
              height: 30,
              padding: const EdgeInsets.all(8),
              margin: const EdgeInsets.only(right: 10),
              decoration: BoxDecoration(color: const Color(0xffe8e8e8), borderRadius: BorderRadius.circular(4)),
              child: buildAssetImage("icon_document.png", 0, Colors.black),
            ),
            Expanded(
              child: Text(library.name,
                  style: const TextStyle(fontSize: 14, color: Colors.black), maxLines: 1, overflow: TextOverflow.ellipsis),
            ),
            InkWell(onTap: onRemove, child: buildAssetImage("icon_delete.png", 20, Colors.black)),
          ],
        ),
      ),
    );
  }
}
