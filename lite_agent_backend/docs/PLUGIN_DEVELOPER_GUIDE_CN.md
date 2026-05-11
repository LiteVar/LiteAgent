# LiteAgent 插件开发者指南

[English](PLUGIN_DEVELOPER_GUIDE.md) · 中文

本文档面向插件开发者，描述插件服务需要实现的接口、鉴权方式与打包规范，确保在 LiteAgent 中使用插件服务。

## 目录
- [插件定位](#插件定位)
- [插件与 Connector 关系](#插件与-connector-关系)
- [插件服务要求](#插件服务要求)
- [与主服务的配对与鉴权](#与主服务的配对与鉴权)
  - [密钥文件](#密钥文件)
  - [HMAC 鉴权](#hmac-鉴权)
- [接口定义](#接口定义)
  - [健康检查](#健康检查)
  - [获取 Schema](#获取-schema)
  - [获取数据](#获取数据)
  - [保存配置](#保存配置)
  - [设置 connector 状态](#设置-connector-状态)
  - [删除 connector](#删除-connector)
  - [获取用户访问记录统计](#获取用户访问记录统计)
  - [打包规范](#打包规范)

## 插件定位
- 插件服务作为“中转层”，接入第三方平台（如钉钉、公众号、Website 等）。
- 第三方平台请求进入插件服务后，由插件调用 LiteAgent 主服务的 Agent API 完成业务处理。
- 插件的配置页面由 LiteAgent 前端渲染，配置数据由插件服务提供。
- 配置页用于绑定当前 connector 与 agent 的关系,以及一些额外的配置,例如是公众号插件,则需要配置公众号平台的 appId,secret。

## 插件与 Connector 关系
- 插件是服务级别能力，Connector 是插件在某个业务场景下的一条配置实例。
- 一个插件可对应多个 Connector；每个 Connector 由主服务生成唯一的 `connectorId`。
- 插件需以 `connectorId` 为主键保存配置与状态，多个 Connector 之间互不影响。
- Connector 状态通过 `/plugin/status` 控制上线/下线；下线状态不处理第三方平台请求。
- Connector 删除时通过 `/plugin/delete` 清理配置与状态。

## 插件服务要求
- 以 HTTP 服务形式运行在容器内，仅暴露一个服务端口。
- 插件服务必须运行在固定端口 `8888`（容器内部端口），宿主机端口由 runner 负责映射与分配。
- 实现 `/plugin/health`、`/plugin/schema`、`/plugin/data`、`/plugin/config`、`/plugin/status`、`/plugin/delete` 接口。
- 目录与权限约定：
  - 持久化数据目录：`/data/lite-agent-plugin`（仅此目录可写，用于保存重要数据）。
  - 程序包目录：`/opt/lite-agent-plugin/app`（只读，不得把程序包写入持久化数据目录，避免升级回滚到旧包）。
  - 仅开放 `/data/lite-agent-plugin` 与 `/tmp` 的写权限，其余目录必须只读。
  - `/tmp` 使用 `tmpfs`，大小限制 `64MB`。
- 能持久化自身配置数据（主服务不保存插件配置）。
- 需要维护 connector 的上线/下线状态，已下线的 connector 不再处理第三方平台的请求等相关业务。

## 与主服务的配对与鉴权
容器启动时会在 `/data/lite-agent-plugin/key/encryptedKey.txt` 写入 `encryptedKey`，插件读取并解密后用于 HMAC 签名。

请求超时：
- 连接超时 1s
- 读超时 5s
- 不做重试

### 密钥文件
- 文件路径：`/data/lite-agent-plugin/key/encryptedKey.txt`
- 文件内容：AES+Base64 加密后的字符串（`encryptedKey`）
- 解密步骤：
  1) Base64 解码 `encryptedKey`
  2) 使用 AES 解密得到 `sharedKey`
  3) 保存 `sharedKey` 供后续 HMAC 使用
- AES key（硬编码，需与主服务一致）：
  - `UdYbRx48hZQR8cys`

AES 说明：
- 算法/模式/填充：`AES/ECB/PKCS5Padding`
- key 编码：UTF-8
- IV：无
- 密文编码：Base64

### HMAC 鉴权
主服务调用插件服务的所有接口(`/plugin/health`除外)必须携带以下请求头：
- `X-TS`: 毫秒时间戳
- `X-Nonce`: 随机 nonce
- `X-Sign`: HMAC 签名（`HMAC_SHA256(sharedKey, canonical)`）

canonical 串格式：
```
METHOD\nPATH\nTS\nNONCE\nQUERY\nBODY_SHA256\nCONTENT_TYPE
```
说明：
- METHOD 为大写 HTTP 方法（GET/POST）。
- PATH 为请求路径（不含 query）。
- QUERY 为归一化后的 query 参数串（没有 query 时为空字符串）。
- BODY_SHA256 为请求体原始字节的 SHA256（空 body 使用空字符串）。
- CONTENT_TYPE 为请求头 `Content-Type` 原始值（无则空字符串）。

校验规则：
- `X-TS` 在允许时间窗内（建议 10 秒）。
- `X-Nonce` 是否去重由插件自行决定。
- `X-Sign` 与 canonical 串进行 HMAC 签名一致,则通过。

query 归一化规则：
- 按 key 字典序排序（全字段排序，不仅首字母）。
- key 相同则按 value 字典序排序。
- 使用 `key=value` 形式拼接，多个参数用 `&` 连接。
- key 与 value 进行 URL 编码；空值写成 `key=`。

## 接口定义

参数传递规则：
- `GET` 使用 query 参数。
- `POST` 使用 JSON 请求体（`Content-Type: application/json`）。

### 健康检查
`GET /plugin/health?encryptedText=xxx`

用途：
- 提供插件服务可用性检测。

请求参数：
- `encryptedText` 加密字符串

响应示例：
```json
{
  "plainText": "random-text"
}
```

说明：
- `encryptedText` 使用与密钥文件相同的 AES 规则加密。
- 解密步骤：Base64 解码 -> AES 解密（AES/ECB/PKCS5Padding，UTF-8 key）。
- 插件解密后返回原文 `plainText`。

### 获取 schema
`GET /plugin/schema`

用途：
- 返回前端渲染配置页表单所需的 JSON Schema。

请求参数：无

响应示例：
```json
{}
```

字段说明：
- 表单 Schema JSON 数据

### 获取数据
`GET /plugin/data?connectorId=xxx`

请求参数：
- `connectorId` connectorId

响应示例：
```json
{}
```

说明：
- 用于配置页回显配置数据。
- 表单填写的数据
- 当不存在数据时, 返回空对象。

### 保存配置
`POST /plugin/config`

请求体示例：
```json
{
  "connectorId": "xxx",
  "data": {}
}
```

响应示例：
```json
{
  "success": true,
  "errors": []
}
```

说明：
- 插件需自行持久化 `data`。
- `errors` 用于返回参数校验错误。
- `data` 结构需与 `/plugin/schema` 返回的 `schema` 定义一致。

### 设置 connector 状态
`POST /plugin/status`

请求体示例：
```json
{
  "connectorId": "xxx",
  "offline": true
}
```

响应示例：
```json
{
  "success": true
}
```

说明：
- 插件需按 `offline` 字段更新状态并持久化。
- `offline=true` 表示下线，`offline=false` 表示上线。

### 删除 connector
`POST /plugin/delete`

请求体示例：
```json
{
  "connectorId": "xxx"
}
```

响应示例：
```json
{
  "success": true
}
```

说明：
- 插件需删除该 connector 的配置与状态。
- 要求幂等：重复删除不报错。

### 获取用户访问记录统计
`POST /plugin/analyze`

用途：
- 统计指定时间段内的用户访问数据，用于看板展示等场景。

请求体示例：
```json
{
  "connectorId": "xxx",
  "startTime": "2025-12-01 00:00:00",
  "endTime": "2025-12-31 23:59:59"
}
```

响应示例：
```json
{
  "statistics": {
    "activeUserCount": 12,
    "averageConversationRounds": 3.5,
    "averageSessionsPerActiveUser": 1.4,
    "totalTokenConsumption": 1621
  },
  "activeUserTrend": [
    {
      "time": "2025-12-01 00:00:00",
      "activeUserCount": 1
    }
  ],
  "conversationRoundsDistribution": [
    {
      "roundsRange": "1-2轮",
      "count": 8
    }
  ],
  "sessionDistribution": [
    {
      "sessionRange": "1次",
      "userCount": 9
    }
  ]
}
```

说明：
- `startTime`/`endTime` 为字符串时间，格式建议 `yyyy-MM-dd HH:mm:ss`。
- `statistics` 为统计汇总数据：活跃用户数、平均对话轮数、人均会话数、Token 总消耗。
- `activeUserTrend` 为时间趋势，`time` 为字符串时间。
- `conversationRoundsDistribution` 为对话轮数分布，`roundsRange` 为区间描述。
- `sessionDistribution` 为会话次数分布，`sessionRange` 为区间描述。

## 异常处理
异常处理约定：
- 鉴权失败（HMAC/时间窗/nonce）：HTTP 401，返回 `{ "code": "UNAUTHORIZED", "message": "..." }`
- 参数非法：HTTP 400，返回 `{ "code": "INVALID_PARAM", "message": "..." }`
- 系统异常：HTTP 500，返回 `{ "code": "INTERNAL_ERROR", "message": "..." }`
- `/plugin/config` 校验失败: HTTP 500, 返回 `{ "code": 500, "message": '.....' }`

错误码清单（固定值）：
- `UNAUTHORIZED`
- `INVALID_PARAM`
- `INTERNAL_ERROR`

## 打包规范
目前仅需提供 Docker 镜像文件，上传方式以平台要求为准。

镜像内目录约定：
- 程序包目录固定为 `/opt/lite-agent-plugin/app`，镜像内需将应用放在该目录。
- 持久化数据目录固定为 `/data/lite-agent-plugin`，插件运行时仅向该目录写入数据。
- 请勿将程序包或运行时数据写入其他路径，避免只读根文件系统下启动失败或升级回滚。

示例：
```bash
# 构建镜像
docker build -t dingtalk-robot:1.0.0 .
# 导出镜像
docker save -o image.tar dingtalk-robot:1.0.0
```
