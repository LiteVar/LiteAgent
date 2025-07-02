# LiteAgent Java SDK

中文 · [English](README.md)

Java语言版本的 LiteAgent API SDK。

## 功能

- 初始化Agent的会话
- 向Agent发送客户端消息
- 查询历史聊天消息
- 发送Function Call的Callback结果
- 停止当前会话
- 清除当前会话

## 安装

将以下依赖项添加到您的 `pom.xml`：

```xml
<dependency>
    <groupId>com.litevar.liteagent</groupId>
    <artifactId>liteagent_sdk</artifactId>
    <version>0.2.0</version>
</dependency>
```

## 使用方法

```java
//初始化client
LiteAgentClient client = new LiteAgentClient("your-api-key", "baseUrl");

//获取版本
client.getVersion();

//初始化session
client.initSession();

//停止会话
client.stopSession(sessionId);

```


## 许可证

MIT
