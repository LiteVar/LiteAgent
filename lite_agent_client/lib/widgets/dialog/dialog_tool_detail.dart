import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/models/local_data_model.dart';

class ToolDetailDialog extends StatelessWidget {
  final ToolBean tool;

  ToolDetailDialog({required this.tool});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 538,
        height: 438,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.all(Radius.circular(6)),
        ),
        child: Column(
          children: [
            buildTitleContainer(),
            Expanded(
              child: SingleChildScrollView(
                  child: Container(
                margin: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SizedBox(height: 20),
                    Center(
                      child: Column(
                        children: [
                          Container(
                              margin: EdgeInsets.all(20),
                              child: Text(
                                tool.name,
                                style: TextStyle(
                                    fontSize: 14, color: Colors.black),
                              )),
                          Text(
                            tool.description,
                            style: TextStyle(fontSize: 14, color: Colors.black),
                          ),
                          Container(
                              margin: EdgeInsets.symmetric(vertical: 10),
                              child: Divider(height: 0.1))
                        ],
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          "schema:",
                          style: TextStyle(fontSize: 14, color: Colors.black),
                        ),
                        Container(
                          margin: EdgeInsets.all(10),
                          child: Text(
                            tool.schemaText,
                            style: TextStyle(fontSize: 14, color: Colors.grey),
                          ),
                        ),
                        Text(
                          "协议类型:",
                          style: TextStyle(fontSize: 14, color: Colors.black),
                        ),
                        Container(
                            margin: EdgeInsets.all(10),
                            child: Text(
                              tool.schemaType,
                              style:
                                  TextStyle(fontSize: 14, color: Colors.grey),
                            )),
                        Text(
                          "key类型:",
                          style: TextStyle(fontSize: 14, color: Colors.black),
                        ),
                        Container(
                            margin: EdgeInsets.all(10),
                            child: Text(
                              tool.apiType,
                              style:
                                  TextStyle(fontSize: 14, color: Colors.grey),
                            )),
                        Text(
                          "apiKey值:",
                          style: TextStyle(fontSize: 14, color: Colors.black),
                        ),
                        Container(
                            margin: EdgeInsets.all(10),
                            child: Text(
                              tool.apiText,
                              style:
                                  TextStyle(fontSize: 14, color: Colors.grey),
                            )),
                      ],
                    )
                    /*buildToolDesColumn(),
                        buildSchemaInputColumn(),
                        buildAPIInputColumn(),
                        const SizedBox(height: 10),
                        buildBottomButton()*/
                  ],
                ),
              )),
            )
          ],
        ),
      ),
    );
  }

  Container buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(
            topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        const Text("工具详情"),
        const Expanded(child: Spacer()),
        IconButton(
          icon: const Icon(Icons.close, size: 16, color: Colors.black),
          onPressed: () {
            Get.back();
          },
        )
      ]),
    );
  }
}
