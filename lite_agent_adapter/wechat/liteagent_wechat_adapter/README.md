# liteagent-wechat-adapter
English · [中文](README-zh_CN.md)

WeChat adapter for connecting WeChat Official Accounts with the liteagent .

> Version: 0.1.0

## Project Introduction

liteagent-wechat-adapter is a WeChat Official Account adapter developed based on Spring Boot, primarily used to forward messages from WeChat Official Accounts to the liteagent system for processing. This project supports multi-official account configuration, capable of handling messages from different WeChat Official Accounts and routing them to the corresponding liteagent.

### Tech Stack

- Java 17
- Spring Boot 3.5.4
- Maven
- MongoDB
- Redis
- WeChat Official Account Java SDK (weixin-java-sdk)

### Project Structure

```
liteagent-wechat-adapter
├── wechat-adapter-common     # Common module
├── wechat-adapter-modules    # Core business module
└── wechat-adapter-server     # Startup module
```

## WeChat Official Account Integration

### 1. Apply for WeChat Official Account

1. Visit [WeChat Official Platform](https://mp.weixin.qq.com/)
2. Register and select "Service Account"
3. Complete the enterprise verification process
4. Obtain the AppID and AppSecret of the service account

### 2. Add IP Whitelist

1. Log in to the WeChat Official Platform
2. Go to "Development" -> "Basic Configuration"
3. Add the server's public IP address to the "IP Whitelist"
4. Save the configuration

### 3. Enable API Permissions

1. Log in to the WeChat Official Platform
2. Go to "Development" -> "Basic Configuration"
3. Configure the server URL: `http://your-server-address:9082/api/notify/revice/{appId}`
4. Set Token (custom)
5. If encryption is required, set EncodingAESKey
6. Select message encryption method
7. Enable server configuration

Required API permissions:
- Message receiving permission
- Message sending permission

## Project Deployment

### Environment Requirements

- Java 17
- MongoDB
- Redis

### Configuration Files

Configure MongoDB and Redis connection information in `wechat-adapter-server/src/main/resources/application-dev.yml`.

### Docker Deployment

#### Using Docker Compose (Recommended)

Rename the packaged JAR file to **wechat-adapter-server.jar** and copy it to the `script/deploy` directory, then execute the following commands:

```bash
cd script/deploy
docker-compose up -d
```

The service runs on port 9082 by default.

## Access Documentation

After starting the service, you can access the API documentation at `http://localhost:9082/swagger-ui.html`.