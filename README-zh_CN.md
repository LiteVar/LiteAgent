# LiteAgent

[English](README.md) · 中文

LiteAgent是一个面向工业的开源AI Agent平台，旨在帮助用户快速构建和部署AI Agent应用程序。它支持多种模型和工具的集成，提供了Web端、桌面端和后端服务，适用于各种工业场景。

## 程序说明

- [Web端](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_web/README_zh-CN.md)
- [桌面端](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_client/README-zh_CN.md)
- [后端](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_backend/README-zh_CN.md)
- [SDK](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_sdk)

## 特性

### 1. Agent
 
- 普通文字聊天Agent
- 工具Agent
- 多Agent协作支持
  - 普通Agent(Web)
  - 分发Agent(Web, Desktop)
  - 反思Agent(Web, Desktop)
- Agent模式支持：并行、串行、拒绝

### 2. 工具支持

- 协议：openapi、openrpc、openmodbus
- 工具执行模式：并行、串行、拒绝
- 方法序列：工具顺序的反思
- 支持第三方OpenTool回调

### 3. 功能

- 模型管理(Web, Desktop)
- 工具管理(Web, Desktop)
- Agent管理(Web, Desktop)
- 用户管理(Web)
- 知识库管理(Web)

### 4. 模型支持

- 仅支持OpenAI风格API，其他模型(如Zhipu-AI、QianFan、ChatGLM、Chroma、Ollama、qwen等)，请使用例如OpenAPI等工具转换为OpenAI风格。

## 快速开始

### 1. 最低配置

开始使用LiteAgent之前, 请确保您的机器满足以下最低系统要求:

>- CPU >= 4 Core
>- RAM >= 10 GiB

### 2. 配置邀请邮箱

- 配置邀请邮件地址和邮箱账号，以用于创建、邀请用户
- 编辑文件'docker/LiteAgent/config/application.yml', 修改邮件发送地址和账号(host, username, password, port)
```
  mail:
    host: smtp.xxx.com
    username: liteagent@xxx.com
    password: XXXYYYZZZ
    port: 465
```

### 3. 运行服务
#### 3.1 准备
>- 打包构建LiteAgent web(请参考lite_agent_web/README_zh-CN.md), 然后替换到文件夹: docker/nginx/html/
>- 打包构建LiteAgent java后端(请参考lite_agent_backend/README-zh_CN.md), 然后替换到目标文件夹: docker/LiteAgent/app/
#### 3.2 开始
- docker compose拉起服务，请确保您的系统事先已安装docker和docker-compose插件
```
cd docker 
docker compose up -d
```

### 4. 配置初始化账号

1. 打开浏览器，访问初始化页面( [http://<YOUR_IP>:8080/init](http://<YOUR_IP>:8080/init) )
2. 输入邮件地址，昵称，密码和确认密码，点击登录
3. 移动到头像的上方，点击"管理我的Workspace"
4. 之后便可管理您的agent,工具，模型以及用户了
   - 创建Agent的时候，添加模型是必须的，工具是可选的
5. 创建了Agent之后，便可开始聊天

### 5. 新用户登录

- 打开登录页面( [http://<YOUR_IP>:8080](http://<YOUR_IP>:8080) )