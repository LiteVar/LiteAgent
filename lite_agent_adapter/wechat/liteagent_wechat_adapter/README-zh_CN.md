# liteagent-wechat-adapter
[English](README.md) · 中文

微信适配器，用于连接微信服务号与liteagent。

> Version: 0.1.0

## 项目介绍

liteagent-wechat-adapter 是一个基于Spring Boot开发的微信服务号适配器，主要用于将微信服务号的消息转发到liteagent系统进行处理。该项目支持多公众号配置，可以处理来自不同微信服务号的消息，并将其路由到对应的liteagent。

### 技术栈

- Java 17
- Spring Boot 3.5.4
- Maven
- MongoDB
- Redis
- 微信公众号Java SDK (weixin-java-sdk)

### 项目结构

```
liteagent-wechat-adapter
├── wechat-adapter-common     # 通用模块
├── wechat-adapter-modules    # 核心业务模块
└── wechat-adapter-server     # 启动模块
```

## 微信服务号对接

### 1. 申请微信服务号

1. 访问 [微信公众平台](https://mp.weixin.qq.com/) 官网
2. 注册并选择"服务号"
3. 完成企业认证流程
4. 获取服务号的 AppID 和 AppSecret

### 2. 添加白名单

1. 登录微信公众平台
2. 进入"开发" -> "基本配置"
3. 在"IP白名单"中添加服务器公网IP地址
4. 保存配置

### 3. 开通API权限

1. 登录微信公众平台
2. 进入"开发" -> "基本配置"
3. 配置服务器URL: `http://你的服务器地址:9082/api/notify/revice/{appId}`
4. 设置Token（自定义）
5. 如需加密传输，设置EncodingAESKey
6. 选择消息加解密方式
7. 启用服务器配置

需要开通的API权限：
- 接收消息权限
- 发送消息权限

## 项目部署

### 环境要求

- Java 17
- MongoDB
- Redis

### 配置文件

在 `wechat-adapter-server/src/main/resources/application-dev.yml` 中配置MongoDB和Redis连接信息。

### Docker部署

#### 使用Docker Compose (推荐)

将打包后的jar包重命名为**wechat-adapter-server.jar** 复制到`script/deploy`目录下，并执行以下命令

```bash
cd script/deploy
docker-compose up -d
```

服务默认运行在 9082 端口。

## 访问文档

启动服务后，可通过 `http://localhost:9082/swagger-ui.html` 访问API文档。