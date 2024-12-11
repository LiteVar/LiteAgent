English · [中文](README-zh_CN.md)

# LiteAgent
## 1. Program description
  - [web](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_web/README.md)
  - [desktop](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_client/README.md)
  - [backend](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_backend/README.md)

## 2. Quick start
> Before using LiteAgent, make sure your machine meets the following minimum system requirements:
> 
>- CPU >= 2 Core
>- RAM >= 6 GiB

### 2.1 Configure invitation email addresses and email accounts for creating and inviting users

> Edit 'docker/LiteAgent/config/application.yml', change host, username, password and port
```
  mail:
    host: smtp.xxx.com
    username: liteagent@xxx.com
    password: XXXYYYZZZ
    port: 465
```

### 2.2 Pull up services by docker compose

> Please make sure docker, docker compose plugin are installed on your machine
```
cd docker 
docker compose up -d
```

### 2.3 init root user of LiteAgent
1. Open the browser and access the initialization page(http://your_ip:8080/init, please replace with your actual ip or domain name)

![initSuperUser](https://github.com/LiteVar/LiteAgent/blob/master/lite_agent_web/docs/initSuperUser.jpg)

2. Enter email address, nickname, password, and confirm password, click login to create the root user

3. Move to the top of the avatar and click on "Manage My Workspace"

![manageWorkspace](https://github.com/LiteVar/LiteAgent/blob/master/lite_agent_web/docs/open-admin.png)

4. Then you can manage agents, tools, models and users

> When creating an agent, adding a model is necessary and tools are optional

5. After creating the agent, you can start chatting
  
![chatPage](https://github.com/LiteVar/LiteAgent/blob/master/lite_agent_web/docs/chat-page.png)

### 2.4 new user to login

> Access the login page(http://your_ip:8080, please replace with your actual ip or domain name)


# Features

## v0.1.0

### 1. agent
    1. Ordinary text chat agent
    2. tool agent

### 2. Tool protocol support
    1. openapi
	2. openrpc
	3. openmodbus

### 3. Functional modules
    1. LLM management(web, desktop)
    2. Tool management(web, desktop)
    3. Agent management(web, desktop)
    4. User management(web)

### 4. LLM support
	Only support openai，other models(like Zhipu-AI、QianFan、ChatGLM、Chroma、Ollama、qwen and so on), please use the oneapi tool to convert to openai style.

