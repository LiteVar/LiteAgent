[English](README.md) · 中文

# LiteAgent
## 1. 程序说明
  - [web端](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_web/README_zh-CN.md)
  - [桌面端](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_client/README-zh_CN.md)
  - [后端](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_backend/README-zh_CN.md)

## 2. 快速开始
> 开始使用LiteAgent之前, 请确保您的机器满足以下最低系统要求:
> 
>- CPU >= 2 Core
>- RAM >= 6 GiB

### 2.1 先配置邀请邮件地址和邮箱账号，以用于创建、邀请用户

> 编辑文件'docker/LiteAgent/config/application.yml', 修改邮件发送地址和账号(host, username, password, port)
```
  mail:
    host: smtp.xxx.com
    username: liteagent@xxx.com
    password: XXXYYYZZZ
    port: 465
```

### 2.2 docker compose拉起服务

> 请确保您的系统事先已安装docker和docker-compose插件
```
cd docker 
docker compose up -d
```

### 2.3 初始化LiteAgent的root账号
1. 打开浏览器，访问初始化页面(http://your_ip:8080/init, 请使用您的实际ip或域名替换)

![initSuperUser](https://github.com/LiteVar/LiteAgent/blob/master/lite_agent_web/docs/initSuperUser.jpg)

2. 输入邮件地址，昵称，密码和确认密码，点击登录

3. 移动到头像的上方，点击"管理我的Workspace"

![manageWorkspace](https://github.com/LiteVar/LiteAgent/blob/master/lite_agent_web/docs/open-admin.png)

4. 之后便可管理您的agent,工具，模型以及用户了

> 创建agent的时候，添加模型是必须的，工具是可选的

5. 创建了agent之后，便可开始聊天

![chatPage](https://github.com/LiteVar/LiteAgent/blob/master/lite_agent_web/docs/chat-page.png)

### 2.4 新用户登录

> 打开登录页面(http://your_ip:8080, 请使用您的实际ip或域名替换)


# 特性  
## v0.1.0

### 1. agent
    1. 普通文字聊天agent
    2. 工具agent

### 2. 工具协议支持
    1. openapi
	2. openrpc
	3. openmodbus

### 3. 功能
    1. 模型管理(web, desktop)
    2. 工具管理(web, desktop)
    3. agent管理(web, desktop)
    4. 用户管理(web)

### 4. 模型支持
	仅支持openai，其他模型(如Zhipu-AI、QianFan、ChatGLM、Chroma、Ollama、qwen等)，请使用oneapi工具转换为openai风格.
	
## v0.2.0
## 新特性
### 1. agent
- [x] 1.1. agent可使用知识库
- [x] 1.2. multi-agent
  - [x] 1. 普通agent(web)
  - [x] 2. 分发agent(web, desktop)
  - [x] 3. 反思agent(web, desktop)

### 2.agent模式支持
- [x] 1. 并行
- [x] 2. 串行
- [x] 3. 拒绝

### 3. 工具执行模式支持
- [x] 3.1 方法模式：
  - [x] 1. 并行
  - [x] 2. 串行
  - [x] 3. 拒绝
- [x] 3.2 方法序列：工具顺序的反思

### 4. 功能
- [x] 1. 云端agent管理(web, desktop)
- [x] 2. 本地agent管理(web, desktop)
- [x] 3. 用户管理(web)
- [x] 4. 知识库管理（web）

### 5. 其他
- [x] 1. 支持第三方open tool回调