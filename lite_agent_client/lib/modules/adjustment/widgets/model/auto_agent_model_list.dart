import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local/model.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';
import 'model_state_manager.dart';

/// Auto Agent模型列表组件
/// 负责显示和管理Auto Agent的模型列表
class AutoAgentModelList extends StatelessWidget {
  final ModelStateManager modelStateManager;
  final VoidCallback? onBackToModelPage;

  const AutoAgentModelList({
    super.key,
    required this.modelStateManager,
    this.onBackToModelPage,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: const EdgeInsets.only(top: 10),
          child: InkWell(
            onTap: () => modelStateManager.toggleModelExpanded(!modelStateManager.isModelExpanded.value),
            child: Row(children: [
              Obx(() => buildAssetImage(
                modelStateManager.isModelExpanded.value ? "icon_option_expanded.png" : "icon_option_closed.png", 
                18, 
                const Color(0xff333333)
              )),
              const Text("模型", style: TextStyle(fontSize: 14, color: Color(0xff333333)))
            ]),
          ),
        ),
        const SizedBox(height: 10),
        Obx(() => Offstage(
          offstage: !modelStateManager.isModelExpanded.value,
          child: Column(
            children: [
              Container(
                margin: const EdgeInsets.only(left: 18),
                child: const Text("在模型库中对模型开启\"支持 Auto Multi Agent\"后，模型将在此处显示。agent将根据任务自动选择模型。",
                    style: TextStyle(color: Color(0xff999999), fontSize: 12)),
              ),
              Obx(() => Container(
                margin: const EdgeInsets.only(top: 10, left: 10),
                child: Column(children: [
                  if (modelStateManager.autoAgentModelList.isNotEmpty)
                    ...List.generate(
                      !modelStateManager.showMoreModel.value && modelStateManager.autoAgentModelList.length > 2 
                        ? 2 
                        : modelStateManager.autoAgentModelList.length,
                      (index) => _buildModelItem(index, modelStateManager.autoAgentModelList[index]),
                    )
                ]))),
              Obx(() => Offstage(
                offstage: modelStateManager.autoAgentModelList.length <= 2,
                child: InkWell(
                  onTap: () => modelStateManager.toggleShowMoreModel(!modelStateManager.showMoreModel.value),
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 10),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          modelStateManager.showMoreModel.value ? "收起" : "更多",
                          style: const TextStyle(fontSize: 14, color: Color(0xff2A82E4)),
                        ),
                        const SizedBox(width: 10),
                        buildAssetImage("icon_down.png", 12, const Color(0xff2A82E4))
                      ],
                    ),
                  ),
                ),
              )),
              Offstage(
                offstage: modelStateManager.autoAgentModelList.isNotEmpty,
                child: Container(
                  margin: const EdgeInsets.only(left: 18, bottom: 10),
                  child: Row(
                    children: [
                      const Text("还没添加可用模型，", style: TextStyle(color: Color(0xff666666), fontSize: 12)),
                      InkWell(
                        onTap: onBackToModelPage,
                        child: const Text("前往模型管理", style: TextStyle(color: Color(0xff2A82E4), fontSize: 12)),
                      )
                    ],
                  ),
                ),
              )
            ],
          ),
        )),
        horizontalLine(),
      ],
    );
  }

  Widget _buildModelItem(int index, ModelData model) {
    return MouseRegion(
      onEnter: (event) => modelStateManager.hoverModelItem(index.toString()),
      onExit: (event) => modelStateManager.hoverModelItem(""),
      child: Obx(() {
        var isSelect = modelStateManager.modelHoverItemId.value == index.toString();
        var backgroundColor = isSelect ? const Color(0xfff5f5f5) : Colors.transparent;
        return Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(color: backgroundColor, borderRadius: BorderRadius.circular(8)),
          child: Row(children: [
            Container(
              width: 30,
              height: 30,
              padding: const EdgeInsets.all(8),
              margin: const EdgeInsets.only(right: 10),
              decoration: BoxDecoration(color: const Color(0xffe8e8e8), borderRadius: BorderRadius.circular(4)),
              child: buildAssetImage("icon_default_agent.png", 0, Colors.black),
            ),
            Expanded(
              child: Text(
                model.alias ?? "",
                style: const TextStyle(fontSize: 14, color: Colors.black),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ]));
      }),
    );
  }
}
