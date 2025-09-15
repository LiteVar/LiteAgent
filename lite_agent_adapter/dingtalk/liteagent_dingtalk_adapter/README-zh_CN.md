# liteagent-dingtalk-adapter
[English](README.md) · 中文

liteagent的钉钉适配器，用于对接钉钉平台，提供统一的接口服务。


> 版本: 0.1.0

## 项目概述

本项目是liteagent系统的钉钉适配器，旨在简化与钉钉平台的集成工作。通过该适配器，企业系统和第三方开发者可以更方便地调用钉钉API，实现如权限管理、部门用户信息获取等功能。

### 核心功能

- 钉钉应用权限管理
- 部门用户信息获取
- 应用版本信息管理
- 钉钉SDK适配（支持新旧版本）

### 技术架构

- 基于Spring Boot 3.5.4构建后端服务
- 使用Java 17开发
- 采用模块化Maven项目结构
- 集成Sa-Token权限控制框架
- 使用MapStruct Plus进行数据映射
- 集成SpringDoc OpenAPI提供接口文档
- 支持MongoDB和Redisson数据访问
- 同时兼容钉钉新版(2.2.32)和旧版(2.0.0) SDK

## 环境要求

- JDK 17或更高版本
- Maven 3.x
- Redis
- MongoDB

## 项目结构

```
├── dingtalk-adapter-common          # 公共模块
│   ├── dingtalk-adapter-common-auth     # 认证相关模块
│   ├── dingtalk-adapter-common-bom      # BOM依赖管理
│   ├── dingtalk-adapter-common-core     # 核心公共类
│   ├── dingtalk-adapter-common-mongoplus# MongoDB操作封装
│   └── dingtalk-adapter-common-web      # Web层公共类
├── dingtalk-adapter-modules         # 功能模块
│   └── dingtalk-adapter-core            # 核心业务逻辑
├── dingtalk-adapter-server          # 主服务模块
└── doc                              # 文档
```

## 快速开始

### 1. 克隆项目

```bash
git clone <项目地址>
cd liteagent-dingtalk-adapter
```

### 2. 配置环境

修改[dingtalk-adapter-server/src/main/resources/application-dev.yml](file:///D:/Users/27707/project/liteagent-dingtalk-adapter/dingtalk-adapter-server/src/main/resources/application-dev.yml)文件中的数据库连接配置：

```yaml
spring:
  data:
    redis:
      host: your_redis_host
      port: your_redis_port
      password: your_redis_password
      database: your_redis_database
      
mongo-plus:
  data:
    mongodb:
      host: your_mongo_host
      port: your_mongo_port
      database: your_mongo_database
      username: your_mongo_username
      password: your_mongo_password
```

### 3. 构建项目

```bash
mvn clean package
```

### 4. 运行项目

```bash
mvn spring-boot:run
```

或者运行主类[DingtalkAdapterServerApplication.java](file:///D:/Users/27707/project/liteagent-dingtalk-adapter/dingtalk-adapter-server/src/main/java/com/litevar/dingtalk/adapter/DingtalkAdapterServerApplication.java)

## Docker部署

### 使用Docker Compose (推荐)

将打包后的jar包重命名为**dingtalk-adapter-server.jar** 复制到`script/deploy`目录下，并执行以下命令

```bash
cd script/deploy
docker-compose up -d
```


## API文档

项目集成了SpringDoc OpenAPI，启动后可以通过以下地址访问API文档：

```
http://localhost:9080/swagger-ui.html
```

## 配置说明

主要配置文件位于[dingtalk-adapter-server/src/main/resources](/dingtalk-adapter-server/src/main/resources)目录下：

- [application.yml](/dingtalk-adapter-server/src/main/resources/application.yml): 主配置文件
- [application-dev.yml](/dingtalk-adapter-server/src/main/resources/application-dev.yml): 开发环境配置
- [application-test.yml](/dingtalk-adapter-server/src/main/resources/application-test.yml): 测试环境配置

## 技术选型

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.4 | 应用框架 |
| Sa-Token | 1.44.0 | 权限认证框架 |
| MapStruct Plus | 1.4.8 | 对象映射 |
| SpringDoc OpenAPI | 2.8.9 | API文档 |
| Redisson | 3.50.0 | Redis客户端 |
| Hutool | 5.8.39 | 工具类库 |
| MongoDB Plus | 2.1.9 | MongoDB操作封装 |
| 钉钉新版SDK | 2.2.32 | 钉钉API新版SDK |
| 钉钉旧版SDK | 2.0.0 | 钉钉API旧版SDK |

## 钉钉应用配置

### 1. 创建钉钉应用

1. 登录钉钉开放平台控制台: https://open-dev.dingtalk.com/
2. 进入企业内部应用页面，创建应用
3. 获取应用的`Client ID`和`Client Secret`

详细步骤可参考官方文档: [获取用户个人信息](https://open.dingtalk.com/document/orgapp/tutorial-obtaining-user-personal-information#d891055dc8grp)

### 2. 申请应用权限点

在应用的权限管理页面，申请以下权限点:

- qyapi_get_department_list
- qyapi_get_department_member
- Card.Instance.Write
- Card.Streaming.Write


### 3. 配置流式AI卡片模板

将[doc/DingtalkStreamingAICardTemplate/StreamingAICardTemplate.json](doc/DingtalkStreamingAICardTemplate/StreamingAICardTemplate.json)文件中的内容配置为机器人消息卡片模板。