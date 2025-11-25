import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/dto/retrieval_result.dart';
import 'package:lite_agent_client/repositories/library_repository.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import '../../server/api_server/file_server.dart';
import '../../utils/alarm_util.dart';
import 'dialog_markdown_preview.dart';

class RetrievalHistoryDialog extends StatefulWidget {
  final String historyId;
  final String title;

  const RetrievalHistoryDialog({
    super.key,
    required this.historyId,
    required this.title,
  });

  @override
  State<RetrievalHistoryDialog> createState() => _RetrievalHistoryDialogState();
}

class _RetrievalHistoryDialogState extends State<RetrievalHistoryDialog> {
  final LibraryRepository _libraryRepository = LibraryRepository();
  final RxList<RetrievalResultDto> resultList = <RetrievalResultDto>[].obs;
  final RxBool isLoading = true.obs;
  final RxMap<int, bool> expandedItems = <int, bool>{}.obs;

  @override
  void initState() {
    super.initState();
    _loadRetrievalHistory();
  }

  Future<void> _loadRetrievalHistory() async {
    try {
      isLoading.value = true;
      var data = await _libraryRepository.getRetrieveHistory(widget.historyId);
      resultList.assignAll(data);
    } catch (e) {
      // Handle error
    } finally {
      isLoading.value = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 800,
        height: 600,
        decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(16))),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildHeader(),
            Expanded(
              child: Obx(() {
                if (isLoading.value) {
                  return const Center(child: CircularProgressIndicator(valueColor: AlwaysStoppedAnimation<Color>(Color(0xff2A82E4))));
                }

                if (resultList.isEmpty) {
                  return const Center(child: Text("没有找到相关检索结果", style: TextStyle(color: Color(0xff999999), fontSize: 14)));
                }

                return ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: resultList.length,
                    itemBuilder: (context, index) => _buildResultItem(index, resultList[index]),
                    separatorBuilder: (context, index) => const SizedBox(height: 10));
              }),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          const Text("检索内容：", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
          Text(widget.title, style: const TextStyle(fontSize: 14, color: Color(0xff2a82e4))),
          const Spacer(),
          InkWell(child: const Icon(Icons.close, size: 20, color: Colors.black), onTap: () => Get.back()),
        ],
      ),
    );
  }

  Widget _buildResultItem(int index, RetrievalResultDto result) {
    String documentName = result.documentName;
    documentName = documentName.isEmpty ? "链接文档" : documentName;

    return Container(
      decoration: BoxDecoration(color: const Color(0xfff5f5f5), borderRadius: BorderRadius.circular(12)),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text("#${index + 1} | 关联度: ${result.score.toStringAsFixed(2)}", style: const TextStyle(color: Color(0xff2a82e4), fontSize: 12)),
          const SizedBox(height: 8),
          Obx(() {
            bool isExpanded = expandedItems[index] ?? false;
            String content = result.content;

            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  content,
                  style: const TextStyle(color: Color(0xff333333), fontSize: 14),
                  maxLines: isExpanded ? null : 3,
                  overflow: isExpanded ? TextOverflow.visible : TextOverflow.ellipsis,
                ),
                if (_needsExpansion(content) && !isExpanded)
                  Container(
                    margin: const EdgeInsets.only(top: 4),
                    child: InkWell(
                      onTap: () => expandedItems[index] = true,
                      child: const Text("展开", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                    ),
                  ),
                if (isExpanded && _needsExpansion(content))
                  Container(
                    margin: const EdgeInsets.only(top: 4),
                    child: InkWell(
                      onTap: () => expandedItems[index] = false,
                      child: const Text("收起", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                    ),
                  ),
              ],
            );
          }),
          const SizedBox(height: 8),
          Row(
            children: [
              Text("token：${result.tokenCount}", style: const TextStyle(color: Color(0xff999999), fontSize: 12)),
              const SizedBox(width: 20),
              buildAssetImage("icon_library_segment.png", 14, const Color(0xff999999)),
              const SizedBox(width: 4),
              Flexible(
                child: Text(
                  documentName,
                  overflow: TextOverflow.ellipsis,
                  maxLines: 1,
                  style: const TextStyle(color: Color(0xff999999), fontSize: 12),
                ),
              ),
              const SizedBox(width: 20),
              if (result.fileId.isNotEmpty) ...[
                InkWell(
                  onTap: () => _showDocumentDetail(result.fileId, documentName),
                  child: const Text("查看原文", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                ),
                const SizedBox(width: 20),
                InkWell(
                  onTap: () => _downloadDocumentFile(result.fileId),
                  child: const Text("下载源文件", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                )
              ],
            ],
          ),
        ],
      ),
    );
  }

  bool _needsExpansion(String text) {
    // 简单判断是否需要展开：计算文本行数
    const double fontSize = 14.0;
    const double containerWidth = 750; // 大概的容器宽度

    final textPainter = TextPainter(
      text: TextSpan(text: text, style: const TextStyle(fontSize: fontSize)),
      textDirection: TextDirection.ltr,
      maxLines: 3,
    );

    textPainter.layout(maxWidth: containerWidth);
    return textPainter.didExceedMaxLines;
  }

  Future<void> _showDocumentDetail(String fileId, String fileName) async {
    String preview = await libraryRepository.getDocumentPreview(fileId) ?? "";
    if (preview.isNotEmpty) {
      Get.dialog(
        barrierDismissible: false,
        MarkdownPreviewDialog(titleText: fileName, contentText: preview),
      );
    }
  }

  Future<void> _downloadDocumentFile(String fileId) async {
    final result = await FileServer.downloadDatasetSourceFile(fileId: fileId);
    switch (result) {
      case DownloadResult.success:
        AlarmUtil.showAlertToast('下载成功');
        break;
      case DownloadResult.cancelled:
        break;
      case DownloadResult.failed:
        AlarmUtil.showAlertToast('下载失败');
        break;
    }
  }
}
