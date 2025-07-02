# LiteAgent Java SDK

English · [中文](README-zh_CN.md)

Java SDK for integrating with the LiteAgent API.

## Features

- Initialize an Agent session
- Send client messages to the Agent
- Subscribe to Agent messages, including: Agent messages, chunk messages, Function Call messages
- Send Function Call callback results
- Stop the current session
- Clear the current session

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.litevar.liteagent</groupId>
    <artifactId>liteagent_sdk</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Usage
```java
//Initialize the client
LiteAgentClient client = new LiteAgentClient("your-api-key", "baseUrl");

//Get the version
client.getVersion();

//Initialize a session
client.initSession();

//Stop a session
client.stopSession(sessionId);

```

## License

MIT
