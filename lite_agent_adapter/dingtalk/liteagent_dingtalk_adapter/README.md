# liteagent-dingtalk-adapter

English · [中文](README-zh_CN.md)

DingTalk adapter for liteagent, used to integrate with DingTalk platform and provide unified API services.

> Version: 0.1.0

## Project Overview

This project is a DingTalk adapter for the liteagent system, designed to simplify the integration with the DingTalk platform. Through this adapter, enterprise systems and third-party developers can more easily call DingTalk APIs to implement functions such as permission management and department/user information retrieval.

### Core Features

- DingTalk application permission management
- Department and user information retrieval
- Application version information management
- DingTalk SDK adaptation (supporting both new and old versions)

### Technical Architecture

- Backend services built with Spring Boot 3.5.4
- Developed using Java 17
- Modular Maven project structure
- Integrated with Sa-Token for permission control
- Using MapStruct Plus for data mapping
- Integrated with SpringDoc OpenAPI for API documentation
- Supports MongoDB and Redisson data access
- Compatible with both new (2.2.32) and old (2.0.0) versions of DingTalk SDK

## Environment Requirements

- JDK 17 or higher
- Maven 3.x
- Redis
- MongoDB

## Project Structure

```
├── dingtalk-adapter-common          # Common module
│   ├── dingtalk-adapter-common-auth     # Authentication related module
│   ├── dingtalk-adapter-common-bom      # BOM dependency management
│   ├── dingtalk-adapter-common-core     # Core common classes
│   ├── dingtalk-adapter-common-mongoplus# MongoDB operation encapsulation
│   └── dingtalk-adapter-common-web      # Web layer common classes
├── dingtalk-adapter-modules         # Functional modules
│   └── dingtalk-adapter-core            # Core business logic
├── dingtalk-adapter-server          # Main service module
└── doc                              # Documentation
```

## Quick Start

### 1. Clone the Project

```bash
git clone <project-url>
cd liteagent-dingtalk-adapter
```

### 2. Environment Configuration

Modify the database connection configuration in [dingtalk-adapter-server/src/main/resources/application-dev.yml](file:///D:/Users/27707/project/liteagent-dingtalk-adapter/dingtalk-adapter-server/src/main/resources/application-dev.yml):

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

### 3. Build the Project

```bash
mvn clean package
```

### 4. Run the Project

```bash
mvn spring-boot:run
```

Or run the main class [DingtalkAdapterServerApplication.java](/dingtalk-adapter-server/src/main/java/com/litevar/dingtalk/adapter/DingtalkAdapterServerApplication.java)

## Docker Deployment

### Using Docker Compose (Recommended)

Rename the packaged JAR file to **dingtalk-adapter-server.jar** and copy it to the `script/deploy` directory, then execute the following commands:

```bash
cd script/deploy
docker-compose up -d
```


## API Documentation

The project integrates SpringDoc OpenAPI. After startup, you can access the API documentation at:

```
http://localhost:9080/swagger-ui.html
```

## Configuration Instructions

Main configuration files are located in the [dingtalk-adapter-server/src/main/resources](/dingtalk-adapter-server/src/main/resources) directory:

- [application.yml](/dingtalk-adapter-server/src/main/resources/application.yml): Main configuration file
- [application-dev.yml](/dingtalk-adapter-server/src/main/resources/application-dev.yml): Development environment configuration
- [application-test.yml](/dingtalk-adapter-server/src/main/resources/application-test.yml): Test environment configuration

## Technology Stack

| Technology | Version | Description |
|------------|---------|-------------|
| Spring Boot | 3.5.4 | Application framework |
| Sa-Token | 1.44.0 | Authentication framework |
| MapStruct Plus | 1.4.8 | Object mapping |
| SpringDoc OpenAPI | 2.8.9 | API documentation |
| Redisson | 3.50.0 | Redis client |
| Hutool | 5.8.39 | Utility library |
| MongoDB Plus | 2.1.9 | MongoDB operation encapsulation |
| DingTalk New SDK | 2.2.32 | DingTalk API new SDK |
| DingTalk Old SDK | 2.0.0 | DingTalk API old SDK |

## DingTalk Application Configuration

### 1. Create DingTalk Application

1. Log in to DingTalk Open Platform Console: https://open-dev.dingtalk.com/
2. Go to the enterprise internal application page and create an application
3. Obtain the application's `Client ID` and `Client Secret`

For detailed steps, please refer to the official documentation: [Obtaining User Personal Information](https://open.dingtalk.com/document/orgapp/tutorial-obtaining-user-personal-information#d891055dc8grp)

### 2. Apply for Application Permissions

On the application's permission management page, apply for the following permissions:

- qyapi_get_department_list
- qyapi_get_department_member
- Card.Instance.Write
- Card.Streaming.Write

### 3. Configure Streaming AI Card Template

Configure the content of [doc/DingtalkStreamingAICardTemplate/StreamingAICardTemplate.json](doc/DingtalkStreamingAICardTemplate/StreamingAICardTemplate.json) file as the robot message card template.