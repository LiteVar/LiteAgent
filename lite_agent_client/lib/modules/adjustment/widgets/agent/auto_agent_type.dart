import 'package:flutter/material.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

/// Auto Agent类型组件
/// 显示Auto Multi Agent的说明信息
class AutoAgentType extends StatelessWidget {
  const AutoAgentType({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text("类型", style: TextStyle(fontSize: 14, color: Color(0xff333333))),
        Container(
          margin: const EdgeInsets.symmetric(vertical: 10),
          child: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text("Auto Multi Agent", style: TextStyle(fontSize: 14, color: Color(0xff666666))),
              SizedBox(height: 5),
              Text("AI能够理解任务，并从工具库和模型库中，搭建一个临时的agent执行任务，可以精准、高效地达成目标。", style: TextStyle(fontSize: 14, color: Color(0xff999999))),
            ],
          ),
        ),
        horizontalLine(),
        const SizedBox(height: 10),
      ],
    );
  }
}
