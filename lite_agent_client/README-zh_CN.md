# lite_agent_client

[English](README.md) · 中文

本地端：本地Agent自己设置LLM，无需登录即可使用，不涉及workspace的创建。
云端：需要用户登录自己账号、服务器地址，才能显示云端数据。在desktop中云端功能仅提供对话聊天相关功能，其他需要对云端的agents、工具、模型、调试、个人信息修改，在功能触发自动打开系统浏览器，进入用户端界面。

## 如何构建

- macOS

```bash 
flutter build macos --release
```

- Windows

```bash 
flutter build windows --release
```

- Linux

```bash 
flutter build linux --release
```

## 如何使用

- 本地端

1.在大模型Tab中点击“新建模型”按钮，输入大模型接口的名称、BaseUrl、Apikey，完成创建；
2.在Agent Tab中点击“新建本地Agent”按钮，填写Agent信息，完成创建；
3.进入Agent调试页面，选择大模型，添加工具(可选)，点击保存开始调试；
4.完成调试后，回到聊天Tab，点击“开始聊天”选择对应的Agent进行聊天。

- 云端

1.点击“未登录/设置”，输入注册好的帐号、密码、服务器地址;
2.点击“开始聊天”选择云端Agent进行聊天。