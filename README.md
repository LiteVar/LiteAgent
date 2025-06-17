# LiteAgent

English · [中文](README-zh_CN.md)

LiteAgent is an open-source AI Agent platform designed for industrial applications, enabling users to quickly build and deploy AI Agent applications. It supports integration with various models and tools, providing web, desktop, and backend services suitable for diverse industrial scenarios.

## Program description
  - [Web](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_web/README.md)
  - [Desktop](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_client/README.md)
  - [Backend](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_backend/README.md)
  - [SDK](https://github.com/LiteVar/LiteAgent/tree/master/lite_agent_sdk)

## Features

### 1. Agent

- Ordinary text chat agent
- Tool agent
- Multi-agent
- General agent(Web)
- Distribution agent(Web, Desktop)
- Reflection agent(Web, Desktop)
- Agent mode support: Parallel, Serial, Reject

### 2. Tool Support

- Protocol: openapi, openrpc, openmodbus
- Tool mode support: Parallel, Serial, Reject
- Tool Sequence: Reflection on Tool Order
- Supports third-party OpenTool callbacks

### 3. Functional Modules

- LLM management(Web, Desktop)
- Tool management(Web, Desktop)
- Agent management(Web, Desktop)
- User management(Web)
- Knowledge base management(Web)

### 4. LLM support

- Only support OpenAI Style API，other models(like: Zhipu-AI、QianFan、ChatGLM、Chroma、Ollama、qwen and so on), please use the OneAPI tool to convert to OpenAI style.

## Quick Start

### 1. Minimum System
Before using LiteAgent, make sure your machine meets the following minimum system requirements:
 
>- CPU >= 2 Core
>- RAM >= 6 GiB

### 2. Invitation Email Configuration

- Configure invitation email addresses and email accounts for creating and inviting users
- Edit `docker/LiteAgent/config/application.yml`, change host, username, password and port
```
mail:
  host: smtp.xxx.com
  username: liteagent@xxx.com
  password: XXXYYYZZZ
  port: 465
```

### 3. Run

- Pull up services by docker compose
- Please make sure docker, docker compose plugin are installed on your machine
```
cd docker 
docker compose up -d
```

### 4. Init Root User

1. Open the browser and access the initialization page( [http://<YOUR_IP>:8080/init](http://<YOUR_IP>:8080/init) )
2. Enter email address, nickname, password, and confirm password, click login to create the root user
3. Move to the top of the avatar and click on "Manage My Workspace"
4. Then you can manage agents, tools, models and users
   - When creating an agent, adding a model is necessary and tools are optional  
5. After creating the agent, you can start chatting

### 5. New User Login

- Access the login page([http://<YOUR_IP>:8080](http://<YOUR_IP>:8080) )
