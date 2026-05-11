# lite-agent-plugin-runner

English · [中文](README-zh_CN.md)

`lite-agent-plugin-runner` is a Spring Boot service that manages plugin containers on the local Docker daemon. It handles package uploads, container lifecycle operations, and authentication with the main service.

## Features
- Local Docker-only container lifecycle management
- Package upload or remote download support
- Enable/disable plugins, query status, fetch logs
- Docker availability check on startup
- HMAC authentication (upload endpoint excluded)
- File-based pairing with encrypted keys

## Requirements
- Java 17
- Maven 3.x
- Docker (local daemon accessible)

## Build and Run
```bash
mvn clean package
java -jar target/lite-agent-plugin-runner.jar
```

Run locally:
```bash
mvn spring-boot:run
```

## Run with Docker
Build the jar locally first:
```bash
mvn -DskipTests package
```

Change into the jar output directory first:
```bash
cd target
```

Run the container (creates `./data` and `./uploads` under the current directory):
```bash
docker run -d --name lite-agent-plugin-runner -p 18080:8080 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$(pwd)":/home \
  -w /home \
  azul/zulu-openjdk:17-latest \
  java -jar /home/lite-agent-plugin-runner.jar --runner.host-data-dir="$(pwd)/data"
```

Note: the Docker socket mount is required so the runner can manage local containers. When running inside a container, set `runner.host-data-dir` to a host path shared with Docker Desktop.

Windows (PowerShell):
```powershell
docker run -d --name lite-agent-plugin-runner -p 18080:8080 `
  -v //./pipe/docker_engine:/var/run/docker.sock `
  -v ${PWD}:/home `
  -w /home `
  azul/zulu-openjdk:17-latest `
  java -jar /home/lite-agent-plugin-runner.jar --runner.host-data-dir=${PWD}\data
```

Note: use Docker Desktop in Linux containers mode.

## Pairing & Authentication
1. Start the runner. It will generate an `encryptedKey.txt` in the `dataDir` (default `./data`).
2. Copy the content of `encryptedKey.txt` and configure it in the main service.
3. The runner uses this key for HMAC authentication of all management APIs (except health and upload).

## Configuration
Defaults live in `src/main/resources/application.yml`:
- `runner.data-dir`: pairing/runtime data directory (default `./data`)
- `runner.host-data-dir`: host data directory used for Docker bind mounts when the runner runs inside a container
- `runner.upload-dir`: upload directory (default `./uploads`)
- `runner.security.time-window-seconds`: auth time window (default 10s)

Prefer overrides via env vars or `-D` flags instead of editing the defaults.

## API and Security
See `plugin_runner_design.md` for API definitions and auth details.

## Tests
```bash
mvn test
```