# API for LiteAgent Server

版本：`1.0.0`

## 目录
- HTTP API
  - [/version](#1-获取版本)：获取当前服务器版本。
  - [/initSession](#2-初始化agent会话)：初始化SessionAgent会话，返回SessionId。
  - [/chat](#3-提交用户消息订阅会话执行过程)：提交用户消息，订阅会话执行过程。
  - [/callback](#4-响应functioncall结果)：客户端接收到FunctionCall要求后，调用此接口返回结果。
  - [/history](#5-获取历史消息)：取某个会话的历史消息列表。
  - [/stop](#6-停止会话)：通过指定 sessionId 停止对应的会话操作。
  - [/clear](#7-清除会话数据)：清除会话数据，返回对应的 sessionId。
- AgentMessage说明
  - [`type`在不同类型下的`content`结构](#1-type在不同类型下的content结构)
  - [`to`为`client`时`content`的几个状态字符串](#2-to为client时content的几个状态字符串)

## HTTP API

### Base URL

```http request
https://api.liteagent.cn/liteAgent/v1
```

### Header
| 名称            | 值              | 说明                            |
|---------------|----------------|-------------------------------|
| Authorization | Bearer sk-xxxx | LiteAgent的Agent编辑页中生成`ApiKey` |

### 1. 获取版本

**请求方法**: `GET`

**路径**: `/version`

**功能说明**: 获取当前服务器版本。

#### 请求参数

无

#### 响应

**状态码**: `200 OK`

**响应体**:

| 字段名       | 类型       | 说明              |
|-----------|----------|-----------------|
| `version` | `string` | 版本号，采用"a.b.c"格式 |

#### 响应示例

```json
  {
      "version": "0.0.0"
  }
```

### 2. 初始化Agent会话

**请求方法**: `POST`

**路径**: `/initSession`

**功能说明**: 初始化SessionAgent会话，返回SessionId。

#### 示例

```http request
POST /initSession
```

#### 响应

**状态码**: `200 OK`

**响应体**:

| 字段名         | 类型       | 说明                                               |
|-------------|----------|--------------------------------------------------|
| `sessionId` | `string` | 会话ID，系统生成的唯一标识符。用以后续对于该session的消息订阅、stop、clear操作 |

#### 响应示例

```json
{
  "sessionId": "1901883204734947328"
}
```

### 3. 提交用户消息，订阅会话执行过程

**请求方法**: `POST`

**路径**: `/chat`

**功能说明**: 提交用户消息，订阅会话执行过程。需通过此接口建立`SSE`连接,后续产生的消息数据都将通过流的形式推送给客户端.

#### 请求参数

##### Query参数

| 字段名         | 类型       | 说明                        |
|-------------|----------|---------------------------|
| `sessionId` | `string` | 通过`initSession`接口获取到的临时会话 |

##### Body参数
`application/json`

| 字段名               | 类型        | 说明                                                                                                                                                                              |
|-------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `content`         | `array`   | 消息内容列表，数组中每个元素为 `object`                                                                                                                                                        |
| `content.type`    | `string`  | 消息类型，可选为 `text` `imageUrl`                                                                                                                                                      |
| `content.message` | `string`  | 消息内容，例如："给我一个随机数"; <br/>如果消息类型为imageUrl，message的String可选为：<br/>1. 图片链接："https://example.com/path/to/image.png" ，且确保公网可访问<br/>2. base64格式："data:image/jpeg;base64,{图片的base64编码}" |
| `stream`          | `boolean` | （可选）用于订阅 LiteAgent返回消息的方式，默认为`false`                                                                                                                                            |

#### 请求示例

```http request
POST /chat?sessionId=1901883204734947328
```

```json
{
  "content": [
    { "type": "text", "message":"你是谁？" }
  ],
  "stream": true
}
```

#### 响应流

- 采用SSE，`Content-Type: text/event-stream`

- 消息类型`event`有两种类型:`message`和`chunk`。其中，`chunk`类型只有`stream`为`true`时，且`role`为`assistant`或`subagent`有效。

当`event`为`message`时，`data`类型为`object`类型，`data`结构如下:

| 字段                                   | 类型        | 说明                                                                                                                        |
|--------------------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------|
| `sessionId`                          | `string`  | 会话id                                                                                                                      |
| `taskId`                             | `string`  | 任务id                                                                                                                      |
| `role`                               | `string`  | 角色，可选值包括： `developer`、`user`、`agent`、`assistant`、`dispatcher`、`subagent`、`reflection`、`tool`、`client`                     |
| `to`                                 | `string`  | 消息发送的目标，可选值包括与`role`一致                                                                                                    |
| `type`                               | `string`  | 消息类型，可能包含的值有:`text`、`imageUrl`、`contentList`、`toolCalls`、`dispatch`、`reflection`、`toolReturn`、`functionCall`、`taskStatus` |
| `content`                            | `dynamic` | 见以下示例定义，详情定义见 [`type`在不同类型下的`content`结构](#1-type在不同类型下的content结构)                                                         |
| `completions`                        | `object`  | （可选）大模型完成的详细信息，包含以下字段：                                                                                                    |
| `completions.usage`                  | `object`  | token 使用信息                                                                                                                |
| `completions.usage.promptTokens`     | `number`  | 提示符 token 数                                                                                                               |
| `completions.usage.completionTokens` | `number`  | 生成 token 数                                                                                                                |
| `completions.usage.totalTokens`      | `number`  | 总 token 数                                                                                                                 |
| `completions.id`                     | `string`  | 大模型返回的消息 ID                                                                                                               |
| `completions.model`                  | `string`  | 大模型名称，例如 gpt-4o-mini                                                                                                      |
| `createTime`                         | `string`  | 消息创建时间，格式为 yyyy-MM-ddTHH:mm:ss.SSSZ                                                                                       |

#### `event`为`message`的`data`示例

- 用户发送的消息：`type`为`contentList`
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"user",
    "to":"agent",
    "type": "contentList",
    "content":[
      { "type": "text", "message":"你是谁？" }
    ],
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

- 当LLM有functionCalling时的消息：`type`=`toolCalls`
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"assistant",
    "to":"agent",
    "type": "toolCalls",
    "content":[
      { "id":"call_abc123", "name":"_ip_local_geo_v1_district", "arguments":{} }
    ],
    "completions": {
      "usage": {
        "promptTokens": 199,
        "completionTokens": 12,
        "totalTokens": 211
      },
      "id": "chatcmpl-9bgYkOjpdtLV0o0JugSmnNzGrRFMG",
      "model": "gpt-4o-mini"
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

- `tool`执行完后，响应给LLM：`type`=`toolReturn`
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"tool",
    "to":"agent",
    "type": "toolReturn",
    "content":{ 
      "id":"functionCallId", 
      "result":{} 
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```
  
- `dispatcher`分发指令到`subagent`
    ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"subagent",
    "to":"agent",
    "type": "dispatch",
    "content":[
      {
        "dispatchId":"ddd",
        "agentId": "xxx", 
        "name": "MyAgent",
        "content": [
          { "type": "text", "message":"查询IP" }
        ]
      }
    ],
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

- `subagent`响应
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"subagent",
    "to":"agent",
    "type": "dispatch",
    "content": {
      "dispatchId":"",
      "agentId": "xxx", 
      "name": "MyAgent",
      "content": [
        { "type": "text", "message":"IP地址为192.168.31.123" }
      ]
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

- 反思请求：`type`=`reflection`
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"reflection",
    "to":"agent",
    "type": "reflection",
    "content": {
      "isPass":"",
      "agentId": "xxx", 
      "name": "MyAgent",
      "messageScore": {
        "content": [
          { "type": "text", "message":"查询IP" }
        ],
        "messageType": "text",
        "message": "查询IP的结果为192.168.31.123",
        "reflectScoreList": [
          {"score": 10, "description": "返回数据包含IP信息"}
        ] 
      },
      "passScore":"8",
      "count": "1", 
      "maxCount": "10"
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

- 当`stream`=`false`，LLM返回的结果
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"assistant",
    "to":"agent",
    "type": "text",
    "content": "查询得到的IP地址为192.168.31.123",
    "completions": {
      "usage": {
        "promptTokens": 199,
        "completionTokens": 12,
        "totalTokens": 211
      },
      "id": "chatcmpl-9bgYkOjpdtLV0o0JugSmnNzGrRFMG",
      "model": "gpt-4o-mini"
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

- `agent`告知`client`当前状态
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"agent",
    "to":"client",
    "type": "taskStatus",
    "content": {
      "status": "start",
      "description": {}
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

当`stream`=`true`时，LLM的消息返回`event`为`chunk`，`role`为`agent` `assistant`或`subagent`。`data`类型为`object`类型，`data`结构如下:

| 字段                                   | 类型       | 说明                                        |
|--------------------------------------|----------|-------------------------------------------|
| `sessionId`                          | `string` | 会话id                                      |
| `taskId`                             | `string` | 任务id                                      |
| `role`                               | `string` | 角色，可选值包括： `agent`、`assistant`、`subagent`、 |
| `to`                                 | `string` | 消息目标，可选值包括与`role`一致                       |
| `type`                               | `string` | 消息类型，可能包含的值有:`text`                       |
| `part`                               | `string` | LLM返回信息的片段                                |
| `completions`                        | `object` | （可选）大模型完成的详细信息，包含以下字段：                    |
| `completions.usage`                  | `object` | token 使用信息                                |
| `completions.usage.promptTokens`     | `number` | 提示符 token 数                               |
| `completions.usage.completionTokens` | `number` | 生成 token 数                                |
| `completions.usage.totalTokens`      | `number` | 总 token 数                                 |
| `completions.id`                     | `string` | 大模型返回的消息 ID                               |
| `completions.model`                  | `string` | 大模型名称，例如 gpt-4o-mini                      |
| `createTime`                         | `string` | 消息创建时间，格式为 yyyy-MM-ddTHH:mm:ss.SSSZ       |

#### `data`为`chunk`示例：

- 
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"assistant",
    "to":"agent",
    "type": "text",
    "part": "您的",
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```
- `chunk`文本片段响应结束
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"assistant",
    "to":"agent",
    "type": "text",
    "part": "",
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

- `chunk`过程的token使用信息
  ```json
  {
    "sessionId":"1901883204734947328",
    "taskId":"1901883415867822080",
    "role":"assistant",
    "to":"agent",
    "type": "text",
    "content": "",
    "completions": {
      "usage": {
        "promptTokens": 199,
        "completionTokens": 12,
        "totalTokens": 211
      },
      "id": "chatcmpl-9bgYkOjpdtLV0o0JugSmnNzGrRFMG",
      "model": "gpt-4o-mini"
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
  ```

### 4. 响应FunctionCall结果

**请求方法**: `POST`

**路径**: `/callback`

**功能说明**: 客户端接收到FunctionCall要求后，调用此接口返回结果。

#### 请求参数

##### Query参数

| 字段名         | 类型       | 说明                        |
|-------------|----------|---------------------------|
| `sessionId` | `string` | 通过`initSession`接口获取到的临时会话 |

##### Body参数
`application/json`

| 字段名      | 类型                         |   | 说明                                      |
|----------|----------------------------|:--|-----------------------------------------|
| `id`     | `string`                   |   | 来自FunctionCall.id，用于对应返回的FunctionCall结果 |
| `result` | Map\[`string`, `dynamic`\] |   | 工具返回的结果map                              |

#### 请求示例

```json
{
  "id":"call_z5FK2dAfU8TXzn61IJXzRl5I",
  "result": {
    "status":"operate successfully."
  }
}
```

#### 响应

**状态码**: `200 OK`

**响应体**: 

无

### 5. 获取历史消息

**请求方法**: `GET`

**路径**: `/history`

**功能说明**: 获取某个会话的历史消息列表。

#### 请求参数

##### Query参数

| 字段名         | 类型       | 说明                     |
|-------------|----------|------------------------|
| `sessionId` | `string` | 会话 ID，读取对应Agent的当前会话历史 |

#### 请求示例

```http request
GET /history?sessionId=1901883204734947328
```

#### 响应

**状态码**: `200 OK`

**响应体**: `array`类型，对应的`ojbect`为：

| 字段名                                  | 类型       | 说明                                                                                                          |
|--------------------------------------|----------|-------------------------------------------------------------------------------------------------------------|
| `sessionId`                          | `string` | 会话 ID，用于标识对应会话                                                                                              |
| `taskId`                             | `string` | 任务 ID，识别该任务                                                                                                 |
| `role`                               | `string` | 消息来源，可能的值包括：`developer`、`user`、`agent`、`assistant`、`dispatcher`、`subagent`、`reflection`、`tool`、`client`     |
| `to`                                 | `string` | 消息目标，可能的值与`role`一致                                                                                          |
| `type`                               | `string` | 消息类型，可能的值包括：`text`、`imageUrl`、`contentList`、`toolCalls`、`dispatch`、`reflection`、`toolReturn`、`functionCall` |
| `message`                            | `any`    | 消息内容，具体类型需根据 `type` 字段解析                                                                                    |
| `completions`                        | `object` | 大模型完成的详细信息，包含以下字段：                                                                                          |
| `completions.usage`                  | `object` | Token 使用信息                                                                                                  |
| `completions.usage.promptTokens`     | `number` | 提示符 token 数                                                                                                 |
| `completions.usage.completionTokens` | `number` | 生成 token 数                                                                                                  |
| `completions.usage.totalTokens`      | `number` | 总 token 数                                                                                                   |
| `completions.id`                     | `string` | 大模型返回的消息 ID                                                                                                 |
| `completions.model`                  | `string` | 大模型名称，例如 gpt-3.5-turbo                                                                                      |
| `createTime`                         | `string` | 消息创建时间，格式为 yyyy-MM-ddTHH:mm:ss.SSSZ                                                                         |

#### 响应示例

```json
[
  {
    "sessionId": "1901883204734947328",
    "taskId": "1901883415867822080",
    "role": "developer",
    "to": "user",
    "type": "text",
    "message": "This is an example message",
    "completions": {
      "usage": {
        "promptTokens": 100,
        "completionTokens": 522,
        "totalTokens": 622
      },
      "id": "chatcmpl-9bgYkOjpdtLV0o0JugSmnNzGrRFMG",
      "model": "gpt-3.5-turbo"
    },
    "createTime": "2023-06-18T15:45:30.000+0800"
  }
]
```

### 6. 停止会话

**请求方法**: `GET`

**路径**: `/stop`

**功能说明**: 通过指定 `sessionId` 停止对应的会话操作。

#### 请求参数

##### Query参数

| 字段名         | 类型       | 说明                                     |
|-------------|----------|----------------------------------------|
| `sessionId` | `string` | 会话 ID，用于标识需要停止的会话                      |
| `taskId`    | `string` | （可选）任务 ID，用于标识需要停止的任务，不传入责默认停止当前会话所有任务 |

#### 请求示例

- 停止整个会话
```http request
GET /stop?sessionId=1901883204734947328
```

- 停止会话中某个任务
```http request
GET /stop?sessionId=1901883204734947328&taskId=1901883415867822080
```

#### 响应

**状态码**: `200 OK`

**响应体**:

| 字段名         | 类型       | 说明                   |
|-------------|----------|----------------------|
| `sessionId` | `string` | 会话 ID，用于确认停止的会话      |
| `taskId`    | `string` | 任务 ID， 如果停止的是任务，才会返回 |

#### 响应示例

```json
{
  "sessionId": "1901883204734947328",
  "taskId": "1901883415867822080"
}
```

### 7. 清除会话数据

**请求方法**: `GET`

**路径**: `/clear`

**功能说明**: 清除会话数据，返回对应的 sessionId，不支持 SimpleAgent。

#### 请求参数
##### Query参数

| 字段名         | 类型       | 说明                   |
|-------------|----------|----------------------|
| `sessionId` | `string` | 会话 ID，用于标识需要清除上下文的会话 |

#### 请求示例

```http request
GET /clear?sessionId=1901883204734947328
```

#### 响应

**状态码**: `200 OK`

**响应体**:

| 字段名         | 类型       | 说明              |
|-------------|----------|-----------------|
| `sessionId` | `string` | 会话 ID，用于确认清除的会话 |

#### 响应示例

```json
{
    "id": "1901883204734947328"
}
```

## AgentMessage说明

### 1. `type`在不同类型下的`content`结构

- `type`为`text`或`imageUrl`

| `type`     | content数据类型 | 说明  |
|------------|-------------|-----|
| `text`     | `string`    | 纯文本 |
| `imageUrl` | `string`    | 纯文本 |

- `type`为`toolCalls`，类型为`array`，元素结构为：

| `type`       | content数据类型                | 说明                            |
|--------------|----------------------------|-------------------------------|
| `id`         | `string`                   | 大模型返回的 `function calling` 的id |
| `name`       | `string`                   | `function`的名称                 |
| `parameters` | Map\[`string`, `dynamic`\] | 大模型返回的参数map                   |

- `type`为`toolReturn`，类型为`object`，元素结构为：

| `type`   | content数据类型                | 说明                  |
|----------|----------------------------|---------------------|
| `id`     | `string`                   | 对应`functionCall.id` |
| `result` | Map\[`string`, `dynamic`\] | 工具返回的结果             |

- `type`为`contentList`，类型为`array`，元素结构为：

| `type`    | content数据类型 | 说明                                                                                                                                                                              |
|-----------|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `type`    | `string`    | 可选类型为`text`、`imageUrl`                                                                                                                                                          |
| `message` | `string`    | 消息内容，例如："给我一个随机数"; <br/>如果消息类型为imageUrl，message的String可选为：<br/>1. 图片链接："https://example.com/path/to/image.png" ，且确保公网可访问<br/>2. base64格式："data:image/jpeg;base64,{图片的base64编码}" |

- `type`为`reflection`，类型为`object`，元素结构为：

| `type`                                      | content数据类型 | 说明                                                                                                                                                                              |
|---------------------------------------------|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `isPass`                                    | `boolean`   | 反思是否通过                                                                                                                                                                          |
| `messageScore`                              | `object`    | 当次反思的原文及分数                                                                                                                                                                      |
| `messageScore.content`                      | `array`     | 记录用户的原始消息                                                                                                                                                                       |
| `messageScore.content.type`                 | `string`    | 可选类型为`text`、`imageUrl`                                                                                                                                                          |
| `messageScore.content.message`              | `string`    | 消息内容，例如："给我一个随机数"; <br/>如果消息类型为imageUrl，message的String可选为：<br/>1. 图片链接："https://example.com/path/to/image.png" ，且确保公网可访问<br/>2. base64格式："data:image/jpeg;base64,{图片的base64编码}" |
| `messageScore.messageType`                  | `string`    | 可选类型为`text`、`functionCalling`                                                                                                                                                   |
| `messageScore.message`                      | `string`    | 根据messageType不同而不同                                                                                                                                                              |
| `messageScore.reflectScoreList`             | `array`     | 不同反思agent得到的分数的数组，元素类型为`object`                                                                                                                                                 |
| `messageScore.reflectScoreList.score`       | `int`       | 分数                                                                                                                                                                              |
| `messageScore.reflectScoreList.description` | `string`    | （可选）得分描述                                                                                                                                                                        |
| `passScore`                                 | `int`       | 合格的分数，默认为8                                                                                                                                                                      |
| `count`                                     | `int`       | 当前反思的迭代次数                                                                                                                                                                       |
| `maxCount`                                  | `int`       | 最大允许的反思迭代次数，默认为10                                                                                                                                                               |

- `type`为`taskStatus`，类型为`object`，元素结构为：

| `type`        | content数据类型                | 说明                                                              |
|---------------|----------------------------|-----------------------------------------------------------------|
| `status`      | `string`                   | 可选类型为`start`、`stop`、`done`、`toolsStart`、`toolsDone`、`exception` |
| `description` | Map\[`string`, `dynamic`\] | 消息的描述，一般为functionCall时、`exception`时有值                           |

- `type`为`dispatch`，类型为`object`，元素结构为：

| `type`                | content数据类型 | 说明                                                                                                                                                                              |
|-----------------------|-------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `dispatchId`          | `string`    | 纯文本                                                                                                                                                                             |
| `agentId`             | `string`    | 纯文本，可以为 图片链接格式 或 base64格式                                                                                                                                                       |
| `name`                | `array`     | 大模型 `function calling` 返回的内容，元素类型为`object`                                                                                                                                      |
| `contentList`         | `array`     | 记录用户的原始消息                                                                                                                                                                       |
| `contentList.type`    | `string`    | 可选类型为`text`、`imageUrl`                                                                                                                                                          |
| `contentList.message` | `string`    | 消息内容，例如："给我一个随机数"; <br/>如果消息类型为imageUrl，message的String可选为：<br/>1. 图片链接："https://example.com/path/to/image.png" ，且确保公网可访问<br/>2. base64格式："data:image/jpeg;base64,{图片的base64编码}" |

- `type`为`funtionCall`，类型为`object`，元素结构为：

| `type`       | content数据类型                | 说明                            |
|--------------|----------------------------|-------------------------------|
| `id`         | `string`                   | 大模型返回的 `function calling` 的id |
| `name`       | `string`                   | `function`的名称                 |
| `parameters` | Map\[`string`, `dynamic`\] | 大模型返回的参数map                   |

### 2. `to`为`client`时，content的几个状态字符串

在`taskStatus.status`中：

- `"start"`: agent接收到来自`user`的`contentList`，准备处理
- `"stop"`: agent接收到`stop`或者`clear`指令，停止任务
- `"done"`: agent处理完成
- `"exception"`: 处理过程中发生异常
- `"toolsStart"`: 准备提交Tools执行
- `"toolsDone"`: Tools执行完毕
