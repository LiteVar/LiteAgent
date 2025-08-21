import 'package:flutter/material.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import '../../utils/log_util.dart';
import 'package:lite_agent_client/models/dto/account.dart';
import 'package:lite_agent_client/models/dto/workspace.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/utils/extension/function_extension.dart';
import 'package:lite_agent_client/utils/extension/string_extension.dart';
import 'package:lite_agent_client/utils/web_util.dart';
import 'package:lite_agent_client/widgets/common_widget.dart';

import 'dialog_common_confirm.dart';

class UserSettingDialog extends StatelessWidget {
  String server = "";
  WorkSpaceDTO? workSpace;
  var account = Rx<AccountDTO?>(null);
  var accountAvatar = "";

  void initData() async {
    var list = await accountRepository.getAccountWorkspaceList();
    String workSpaceId = await accountRepository.getWorkSpaceId();
    Log.d("workSpaceId:$workSpaceId");
    if (list != null && list.isNotEmpty) {
      //workSpace = list[0];
      for (var workSpace in list) {
        if (workSpaceId == workSpace.id) {
          this.workSpace = workSpace;
          break;
        }
      }
    }
    server = await accountRepository.getApiServerUrl();
    account.value = await accountRepository.getAccountInfoFromBox();
    if (account.value != null) {
      accountAvatar = await account.value?.avatar?.fillPicLinkPrefix() ?? "";
    }
    account.refresh();
  }

  @override
  Widget build(BuildContext context) {
    initData();
    return Center(
        child: Container(
            width: 538,
            height: 527,
            decoration: const BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.all(Radius.circular(6)),
            ),
            child: Column(
              children: [
                _buildTitleContainer(),
                Container(
                  margin: const EdgeInsets.fromLTRB(30, 20, 30, 20),
                  child: Column(children: [
                    Obx(() {
                      var account = this.account.value;
                      return Column(
                        children: [
                          buildProfileRow(accountAvatar),
                          buildItem("昵称:", account?.name ?? "", false, true),
                          buildItem("工作空间:", workSpace?.name ?? "", true, true),
                          buildItem("账号:", account?.email ?? "", true, false),
                          buildItem("服务器地址:", server, true, true),
                        ],
                      );
                    }),
                    buildPasswordColumn(),
                    Row(children: [
                      Container(
                          margin: const EdgeInsets.only(left: 100),
                          child: InkWell(
                            onTap: () => showLogoutDialog(),
                            child: const Text("退出登录", style: TextStyle(color: Colors.red, fontSize: 14)),
                          ))
                    ]),
                  ]),
                )
              ],
            )));
  }

  Column buildPasswordColumn() {
    return Column(children: [
      const SizedBox(height: 20),
      Row(children: [
        const SizedBox(width: 80, child: Row(children: [Spacer(), Text("密码:", style: TextStyle(color: Colors.black, fontSize: 14))])),
        Container(
          margin: const EdgeInsets.only(left: 20),
          child: InkWell(
            onTap: () {
              WebUtil.openUserSettingUrl();
            }.throttle(),
            child: const Text("修改密码", style: TextStyle(color: Colors.blue, fontSize: 14)),
          ),
        )
      ]),
      Container(margin: const EdgeInsets.symmetric(vertical: 20), child: const Divider(height: 0.01, color: Colors.grey))
    ]);
  }

  Row buildProfileRow(String iconUrl) {
    return Row(children: [
      SizedBox(
          width: 80,
          child: Row(children: [
            const Spacer(),
            Container(
              margin: const EdgeInsets.only(top: 40),
              child: const Text("头像:", style: TextStyle(color: Colors.black, fontSize: 14)),
            )
          ])),
      const SizedBox(width: 12),
      SizedBox(height: 68, width: 68, child: buildUserProfileImage(iconUrl)),
      const Spacer(),
      Container(
        margin: const EdgeInsets.only(bottom: 40),
        child: InkWell(
          onTap: () {
            WebUtil.openUserSettingUrl();
          }.throttle(),
          child: const Text("编辑", style: TextStyle(color: Colors.blue, fontSize: 14)),
        ),
      )
    ]);
  }

  Widget buildItem(String title, String content, bool needBorder, bool showBottomLine) {
    return Column(
      children: [
        const SizedBox(height: 20),
        Row(children: [
          SizedBox(
              width: 80,
              child: Row(children: [
                const Spacer(),
                Text(title, style: const TextStyle(color: Colors.black, fontSize: 14)),
              ])),
          const SizedBox(width: 12),
          if (needBorder)
            Container(
              width: 286,
              height: 33,
              padding: const EdgeInsets.only(left: 8),
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey),
                borderRadius: const BorderRadius.all(Radius.circular(8)),
              ),
              child: Row(children: [
                Text(content, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.grey, fontSize: 14))
              ]),
            )
          else
            Expanded(
                child: Container(
                    margin: const EdgeInsets.only(left: 8),
                    child: Text(content,
                        maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.grey, fontSize: 14)))),
        ]),
        if (showBottomLine)
          Container(
            margin: const EdgeInsets.only(top: 20),
            child: const Divider(height: 0.01, color: Colors.grey),
          ),
      ],
    );
  }

  Container _buildTitleContainer() {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      decoration: const BoxDecoration(
        color: Color(0xFFf5f5f5),
        borderRadius: BorderRadius.only(topLeft: Radius.circular(6), topRight: Radius.circular(6)),
      ),
      child: Row(children: [
        const Text("设置"),
        const Spacer(),
        IconButton(icon: const Icon(Icons.close, size: 16, color: Colors.black), onPressed: () => Get.back())
      ]),
    );
  }

  void showLogoutDialog() {
    Get.dialog(
      barrierDismissible: false,
      CommonConfirmDialog(title: "退出登录", content: "确定退出？", confirmString: "", onConfirmCallback: logout),
    );
  }

  Future<void> logout() async {
    try {
      EasyLoading.show(status: "正在登出中...");
      await accountRepository.logout();
      EasyLoading.dismiss();
    } catch (e) {
      EasyLoading.dismiss();
    }
    Get.back();
  }
}
