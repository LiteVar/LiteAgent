# lite-agent-plugin-runner

[English](README.md) · 中文

`lite-agent-plugin-runner` 是一个 Spring Boot 服务，用于在本机 Docker 上管理插件容器。它负责镜像包上传、容器启停与状态查询，并与主服务完成配对与鉴权。

## 功能概览
- 仅操作本机 Docker，管理插件容器生命周期
- 支持上传镜像包或通过 URL 远程下载镜像
- 启用/关闭插件、查询状态、获取日志
- 启动时进行 Docker 可用性检查
- 使用基于文件的加密密钥进行 HMAC 鉴权（上传接口除外）

## 环境要求
- Java 17
- Maven 3.x
- Docker（本机可用）

## 构建与运行
```bash
mvn clean package
java -jar target/lite-agent-plugin-runner.jar
```

本地运行：
```bash
mvn spring-boot:run
```

## 使用 Docker 运行
先在本地构建 jar 包：
```bash
mvn -DskipTests package
```

切换到 jar 输出目录：
```bash
cd target
```

运行容器（会在当前目录创建 `./data` 和 `./uploads`）：
```bash
docker run -d --name lite-agent-plugin-runner -p 18080:8080 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$(pwd)":/home \
  -w /home \
  azul/zulu-openjdk:17-latest \
  java -jar /home/lite-agent-plugin-runner.jar --runner.host-data-dir="$(pwd)/data"
```

说明：需要挂载 Docker socket，runner 才能管理本机 Docker 容器。runner 在容器内运行时需设置 `runner.host-data-dir` 为宿主机共享目录。

Windows（PowerShell）：
```powershell
docker run -d --name lite-agent-plugin-runner -p 18080:8080 `
  -v //./pipe/docker_engine:/var/run/docker.sock `
  -v ${PWD}:/home `
  -w /home `
  azul/zulu-openjdk:17-latest `
  java -jar /home/lite-agent-plugin-runner.jar --runner.host-data-dir=${PWD}\data
```

说明：请使用 Docker Desktop 的 Linux containers 模式。

## 配对与鉴权
1. 启动 runner，它会在 `dataDir`（默认 `./data`）下生成 `encryptedKey.txt`。
2. 复制 `encryptedKey.txt` 中的内容并配置到主服务中。
3. runner 将使用此密钥对所有管理接口（健康检查和上传接口除外）进行 HMAC 签名校验。

## 配置
默认配置位于 `src/main/resources/application.yml`：
- `runner.data-dir`：配对信息与运行时记录目录（默认 `./data`）
- `runner.host-data-dir`：runner 在容器内运行时用于 Docker bind 的宿主机数据目录
- `runner.upload-dir`：插件包上传目录（默认 `./uploads`）
- `runner.security.time-window-seconds`：鉴权时间窗（默认 10 秒）

建议使用环境变量或 `-D` 参数覆盖默认配置，而不是直接修改配置文件。

## 接口与安全
接口定义与鉴权细节请参考 `plugin_runner_design.md`。

## 测试
```bash
mvn test
```