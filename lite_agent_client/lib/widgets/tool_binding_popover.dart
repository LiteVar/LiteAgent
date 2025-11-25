import 'package:flutter/material.dart';
import 'package:lite_agent_client/models/local/function.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

class ToolBindingPopover extends StatefulWidget {
  final ToolFunctionModel function;
  final Function(ToolFunctionModel) onOpenBindingDialog;
  final Function(ToolFunctionModel) onUnbindAgent;

  const ToolBindingPopover({
    super.key,
    required this.function,
    required this.onOpenBindingDialog,
    required this.onUnbindAgent,
  });

  @override
  State<ToolBindingPopover> createState() => _ToolBindingPopoverState();
}

class _ToolBindingPopoverState extends State<ToolBindingPopover> {
  OverlayEntry? _overlayEntry;
  final LayerLink _layerLink = LayerLink();
  bool _isHoveringTarget = false;
  bool _isHoveringPopover = false;

  @override
  void dispose() {
    _removeOverlay();
    super.dispose();
  }

  void _removeOverlay() {
    _overlayEntry?.remove();
    _overlayEntry = null;
  }

  void _showPopover() {
    if (_overlayEntry != null) return;

    _overlayEntry = OverlayEntry(builder: (context) => _buildPopoverContent());

    Overlay.of(context).insert(_overlayEntry!);
  }

  void _checkShouldHide() {
    // 延迟检查，给鼠标移动留出时间
    Future.delayed(const Duration(milliseconds: 100), () {
      if (!_isHoveringTarget && !_isHoveringPopover) {
        _removeOverlay();
      }
    });
  }

  Widget _buildPopoverContent() {
    return Positioned(
      width: 200,
      child: CompositedTransformFollower(
        link: _layerLink,
        showWhenUnlinked: false,
        offset: const Offset(0, 30),
        child: MouseRegion(
          onEnter: (_) {
            _isHoveringPopover = true;
          },
          onExit: (_) {
            _isHoveringPopover = false;
            _checkShouldHide();
          },
          child: Material(
            elevation: 8,
            borderRadius: BorderRadius.circular(8),
            child: Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                  color: Colors.white, borderRadius: BorderRadius.circular(8), border: Border.all(color: const Color(0xffe0e0e0))),
              child: widget.function.isBound ? _buildBoundContent() : _buildUnboundContent(),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBoundContent() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Agent信息
        Row(
          children: [
            Container(
              width: 20,
              height: 20,
              decoration: BoxDecoration(color: const Color(0xffe8e8e8), borderRadius: BorderRadius.circular(4)),
              child: buildAgentProfileImage(widget.function.boundAgentId ?? ""),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(widget.function.boundAgentName ?? "未知Agent",
                  style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500), maxLines: 1, overflow: TextOverflow.ellipsis),
            ),
          ],
        ),
        const SizedBox(height: 6),
        // 触发方式
        Text("触发方式: ${widget.function.triggerMethod ?? "先执行工具"}", style: const TextStyle(fontSize: 11, color: Color(0xff666666))),
        const SizedBox(height: 8),
        // 按钮
        Row(
          children: [
            Expanded(
              child: InkWell(
                onTap: () {
                  _removeOverlay();
                  widget.onOpenBindingDialog(widget.function);
                },
                child: Container(
                  height: 24,
                  decoration: BoxDecoration(color: const Color(0xff2A82E4), borderRadius: BorderRadius.circular(4)),
                  child: const Center(child: Text("编辑", style: TextStyle(fontSize: 11, color: Colors.white))),
                ),
              ),
            ),
            const SizedBox(width: 6),
            Expanded(
              child: InkWell(
                onTap: () {
                  _removeOverlay();
                  widget.onUnbindAgent(widget.function);
                },
                child: Container(
                  height: 24,
                  decoration: BoxDecoration(color: const Color(0xffFF4D4F), borderRadius: BorderRadius.circular(4)),
                  child: const Center(child: Text("解绑", style: TextStyle(fontSize: 11, color: Colors.white))),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildUnboundContent() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Text("还没有绑定agent", style: TextStyle(fontSize: 12, color: Color(0xff666666))),
        const SizedBox(height: 8),
        InkWell(
          onTap: () {
            _removeOverlay();
            widget.onOpenBindingDialog(widget.function);
          },
          child: Container(
            width: double.infinity,
            height: 24,
            decoration: BoxDecoration(color: const Color(0xff2A82E4), borderRadius: BorderRadius.circular(4)),
            child: const Center(child: Text("绑定agent", style: TextStyle(fontSize: 11, color: Colors.white))),
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return CompositedTransformTarget(
      link: _layerLink,
      child: MouseRegion(
        onEnter: (_) {
          _isHoveringTarget = true;
          _showPopover();
        },
        onExit: (_) {
          _isHoveringTarget = false;
          _checkShouldHide();
        },
        child: Container(
          width: 24,
          height: 24,
          decoration: BoxDecoration(
              color: widget.function.isBound ? const Color(0xff4CAF50) : const Color(0xffCCCCCC), borderRadius: BorderRadius.circular(4)),
          child: const Icon(Icons.android, size: 16, color: Colors.white),
        ),
      ),
    );
  }
}
