import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_easyloading/flutter_easyloading.dart';
import 'package:get/get.dart';
import 'package:lite_agent_client/repositories/account_repository.dart';
import 'package:lite_agent_client/utils/alarm_util.dart';
import 'package:lite_agent_client/utils/shared_preferences_uitl.dart';
import 'package:lite_agent_client/utils/web_util.dart';

import '../common_widget.dart';

class LoginDialog extends StatelessWidget {
  final bool noAutoLogin;
  final logic = Get.put(LoginDialogController());

  LoginDialog({this.noAutoLogin = false});

  @override
  Widget build(BuildContext context) {
    logic.initData(noAutoLogin);
    return Center(
      child: KeyboardListener(
        onKeyEvent: (event) {
          if (event.logicalKey == LogicalKeyboardKey.enter && logic.checkClickEnable()) {
            logic.onLoginButtonClick();
          }
        },
        focusNode: FocusNode(),
        child: Container(
          width: 538,
          height: 447,
          padding: const EdgeInsets.all(10),
          decoration: const BoxDecoration(color: Colors.white, borderRadius: BorderRadius.all(Radius.circular(6))),
          child: Column(children: [
            buildCloseButtonRow(),
            buildLogoRow(),
            buildInputContainer(Icons.person, "账户", false, logic.emailController, null),
            Obx(() => buildInputContainer(Icons.lock, "密码", logic.isObscureText.value, logic.passwordController, buildPasswordButton())),
            Obx(() => buildInputContainer(null, "服务器地址", false, logic.serverController, buildServerButton())),
            buildLoginOptionContainer(),
            buildLoginButton()
          ]),
        ),
      ),
    );
  }

  InkWell buildPasswordButton() {
    return InkWell(
      onTap: () => logic.isObscureText.value = !logic.isObscureText.value,
      child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 5),
          child: buildAssetImage(logic.isObscureText.value ? "icon_eye_close.png" : "icon_eye.png", 16, const Color(0xff999999))),
    );
  }

  InkWell buildServerButton() {
    return InkWell(
      onTap: () => logic.isHttps.value = !logic.isHttps.value,
      child: Container(
          margin: const EdgeInsets.symmetric(horizontal: 5),
          child: const Icon(Icons.change_circle_outlined, size: 16, color: Color(0xff999999))),
    );
  }

  Widget buildLoginButton() {
    return Obx(() {
      var clickEnable = logic.clickEnable();
      var buttonColor = clickEnable ? Colors.blue : Colors.grey;
      var button = Container(
        width: 280,
        height: 40,
        decoration: BoxDecoration(color: buttonColor, borderRadius: const BorderRadius.all(Radius.circular(4))),
        child: const Center(child: Text("登录", style: TextStyle(fontSize: 18, color: Colors.white))),
      );
      if (clickEnable) {
        return InkWell(
          onTap: () => logic.onLoginButtonClick(),
          child: button,
        );
      } else {
        return button;
      }
    });
  }

  Container buildLoginOptionContainer() {
    return Container(
      width: 280,
      margin: const EdgeInsets.symmetric(vertical: 24),
      child: Row(
        children: [
          Container(
            width: 16,
            height: 16,
            margin: const EdgeInsets.only(right: 10),
            child: Obx(() => Checkbox(
                  value: logic.isAutoLogin.value,
                  activeColor: Colors.blue,
                  checkColor: Colors.white,
                  onChanged: (isCheck) {
                    logic.isAutoLogin.value = isCheck ?? false;
                    SPUtil.setBool(SharedPreferencesUtil.isAutoLogin, logic.isAutoLogin.value);
                  },
                )),
          ),
          const Text("自动登录", style: TextStyle(fontSize: 14, color: Colors.black)),
          const Spacer(),
          InkWell(
            onTap: () async => WebUtil.openUserSettingUrl(),
            child: const Text("忘记密码", style: TextStyle(fontSize: 14, color: Colors.blue)),
          ),
        ],
      ),
    );
  }

  Container buildInputContainer(
      IconData? icon, String hintString, bool isObscureText, TextEditingController controller, Widget? rightWidget) {
    return Container(
      width: 280,
      height: 30,
      padding: const EdgeInsets.symmetric(horizontal: 4),
      margin: const EdgeInsets.only(top: 30),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey),
        borderRadius: const BorderRadius.all(Radius.circular(4)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          if (icon != null)
            Icon(icon, size: 16, color: Colors.blue)
          else
            Text(logic.isHttps.value ? "https://" : "http://", style: const TextStyle(fontSize: 14, color: Colors.grey)),
          Expanded(
            child: TextField(
              controller: controller,
              cursorColor: Colors.blue,
              obscureText: isObscureText,
              maxLines: 1,
              decoration: InputDecoration(
                  hintText: hintString, border: InputBorder.none, contentPadding: const EdgeInsets.only(left: 4), isDense: true),
              onChanged: (content) => logic.checkClickEnable(),
              style: const TextStyle(fontSize: 14),
            ),
          ),
          if (rightWidget != null) rightWidget
        ],
      ),
    );
  }

  Row buildLogoRow() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        buildAssetImage("icon_default_agent.png", 40, Colors.black),
        const SizedBox(width: 20),
        const Text("LiteAgent", style: TextStyle(fontSize: 24, color: Colors.black))
      ],
    );
  }

  Row buildCloseButtonRow() {
    return Row(
      children: [
        const Spacer(),
        InkWell(
          onTap: () => Get.back(),
          child: Container(
            margin: const EdgeInsets.all(8),
            padding: const EdgeInsets.all(2),
            child: const Icon(Icons.close, size: 20, color: Colors.grey),
          ),
        )
      ],
    );
  }
}

class LoginDialogController extends GetxController {
  final TextEditingController emailController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();
  final TextEditingController serverController = TextEditingController();

  var clickEnable = false.obs;
  var isAutoLogin = false.obs;
  var isObscureText = false.obs;
  var isHttps = false.obs;

  Timer? task;

  @override
  void onInit() async {
    super.onInit();
  }

  void initData(bool noAutoLogin) async {
    isObscureText.value = true;
    var isAutoLogin = await SPUtil.isTrue(SharedPreferencesUtil.isAutoLogin);
    this.isAutoLogin.value = isAutoLogin;

    String serverUrl = (await accountRepository.getApiServerUrl());
    if (serverUrl.startsWith("http://")) {
      serverController.text = serverUrl.substring(7);
      isHttps.value = false;
    }
    if (serverUrl.startsWith("https://")) {
      serverController.text = serverUrl.substring(8);
      isHttps.value = true;
    }
    passwordController.text = "";
    emailController.text = (await accountRepository.getAccountInfoFromBox())?.email ?? "";

    if (isAutoLogin) {
      if (!noAutoLogin) {
        String? password = await SPUtil.getString(SharedPreferencesUtil.password);
        if (password != null) {
          passwordController.text = password;
        }
      }
    }
    checkClickEnable();

    if (isAutoLogin && !noAutoLogin) {
      task = Timer(const Duration(milliseconds: 500), () => onLoginButtonClick());
    }
  }

  bool checkClickEnable() {
    String email = emailController.text.trim();
    String password = passwordController.text.trim();
    String server = serverController.text.trim();
    clickEnable.value = email.isNotEmpty && password.isNotEmpty && server.isNotEmpty;
    return clickEnable.value;
  }

  Future<void> onLoginButtonClick() async {
    String email = emailController.text;
    String password = passwordController.text;
    String server = serverController.text;
    EasyLoading.show(status: '登录中...');
    await accountRepository.login(email, password, isHttps.value ? "https://$server" : "http://$server");
    EasyLoading.dismiss();
    if (await accountRepository.isLogin()) {
      SPUtil.setString(SharedPreferencesUtil.password, password);
      //SPUtil.setBool(SharedPreferencesUtil.isAutoLogin, isAutoLogin.value);
      Get.back();
    } else {
      AlarmUtil.showAlertToast("账号、密码或者服务地址错误，请重新输入");
    }
  }

  @override
  void onClose() {
    task?.cancel();
    emailController.dispose();
    passwordController.dispose();
    serverController.dispose();
    super.onClose();
  }
}
