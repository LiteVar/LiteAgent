<div align="center">

# LiteAgent Backend

**面向多 Agent 编排、知识库检索、工具调用和流式对话的 AI 后端**

[English](README.md) · 中文

  <img src="https://img.shields.io/badge/Java-17-437291?style=flat-square" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.4-6DB33F?style=flat-square" alt="Spring Boot 3.5.4" />
  <img src="https://img.shields.io/badge/Maven-Multi--Module-C71A36?style=flat-square" alt="Maven Multi-Module" />
  <img src="https://img.shields.io/badge/MongoDB-4.0%2B-47A248?style=flat-square" alt="MongoDB 4.0+" />
  <img src="https://img.shields.io/badge/Redis-Required-DC382D?style=flat-square" alt="Redis Required" />
  <img src="https://img.shields.io/badge/Qdrant-Vector%20DB-DC244C?style=flat-square" alt="Qdrant Vector DB" />
</div>

> LiteAgent Backend 是一个基于 Spring Boot 3.5.4 和 Java 17 的多模块 AI Agent 后端，覆盖智能体编排、SSE 流式对话、知识库检索、插件连接器、工具调用、文件转换与对外 API。

## 快速导航

- [项目亮点](#项目亮点)
- [Agent 运转说明](#agent-运转说明)
- [技术栈](#技术栈)
- [运行依赖](#运行依赖)
- [快速开始](#快速开始)
- [部署说明](#部署说明)
- [相关文档](#相关文档)

## 项目亮点

| 能力 | 说明                                    |
| --- |---------------------------------------|
| 多 Agent 编排 | 支持规划、分发、反思、串并行执行和 DAG 任务调度            |
| 流式交互 | 基于 SSE 输出对话、工具事件和中间过程                 |
| 语音能力 | 支持音频转文字、文字转音频和流式语音接口                  |
| 知识库检索 | 文档切分、摘要、Embedding、Qdrant 召回、检索历史      |
| 工具体系 | 支持 OpenAPI3、JSON-RPC、MCP、OpenTool 等协议 |
| 插件连接器 | 支持通过插件方式连接第三方平台与LiteAgent             |
| 文件处理 | 支持上传、预览、签名访问、Markdown 转换、本地/OSS 存储    |
| 对外集成 | 提供 Agent 开放 API          |

## Agent 运转说明

下图概括了 LiteAgent 后端一次 Agent 请求,各分层如何在调度层的协调下完成任务并输出。

![LiteAgent Platform Architecture](docs/images/liteagent-platform-architecture.svg)

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 | Java 17 |
| 框架 | Spring Boot 3.5.4 |
| Web | Spring MVC + WebFlux |
| AI | Spring AI 1.1.2 |
| 数据库 | MongoDB + Redis |
| 向量库 | Qdrant |
| 工具库 | Hutool 5.8.29 |
| 协议扩展 | MCP SDK、OpenTool |
| 构建 | Maven |

## 运行依赖

### 必需

- JDK 17
- Maven 3.9+
- MongoDB 4.0+
- Redis
- Qdrant

### 可选

- SMTP 邮箱服务
  - 用于邀请成员、验证码、密码找回等邮件场景
- Plugin Runner
  - 当使用插件包执行和插件分发能力时需要

## 快速开始

### 1. 准备数据库和基础服务

准备数据库和基础服务：

- MongoDB：`lite-agent`

并确保 Redis、MongoDB、[Qdrant](https://qdrant.tech/documentation/quick-start/) 可连通。

### 2. 修改配置

配置文件目录：[`lite-agent-rest/src/main/resources`](lite-agent-rest/src/main/resources)

### 3. 构建项目

```bash
mvn clean package -DskipTests
```

打包结果默认位于：

- `lite-agent-rest/target/lite-agent-server.jar`

### 4. 启动服务

```bash
java -jar lite-agent-rest/target/lite-agent-server.jar --spring.profiles.active=local
```

启动类：

- [`LiteAgentRestApplication.java`](lite-agent-rest/src/main/java/com/litevar/agent/rest/LiteAgentRestApplication.java)

## 部署说明

### Docker 示例

```bash
docker pull azul/zulu-openjdk:17-latest
docker run -d \
  --name lite-agent-server \
  -p 8080:8080 \
  -e TZ=Asia/Shanghai \
  -v /your/path/lite-agent-backend:/home/liteAgent \
  azul/zulu-openjdk:17-latest \
  java -jar \
  /home/liteAgent/lite-agent-server.jar \
  --spring.profiles.active=local
```

### Nginx SSE 代理示例

```nginx
location /liteAgent/v1/chat/stream {
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_pass http://127.0.0.1:2205/liteAgent/v1/chat/stream;
}

location /liteAgent/v1/chat {
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_pass http://127.0.0.1:2205/liteAgent/v1/chat;
}
```

## 相关文档

- [插件开发指南](docs/PLUGIN_DEVELOPER_GUIDE_CN.md)

## 提示

从 `3.0.0` 版本开始，LiteAgent Backend 的向量数据库由 `Milvus` 改为 `Qdrant`。

如果需要迁移历史向量数据，可使用：

- [`docs/milvus-qdrant-migrate`](docs/milvus-qdrant-migrate)
