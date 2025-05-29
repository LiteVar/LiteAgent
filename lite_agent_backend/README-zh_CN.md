#### LiteAgent 后端

[English](README.md) · 中文

##### 技术选型
* JDK 17
* Springboot 3.3.1
* LangChain4j 0.35.0
* hutool 5.8.29
* MongoPlus 2.1.6
* Milvus 2.5.6

##### 准备环境
* JDK17
* Maven
* MongoDB(至少4.0以上版本)
* Redis
* 开启了SMTP服务的邮箱(账号、密码、服务IP, port)
* Milvus 2.5.6

##### Milvus 安装部署
1.  按照 Milvus 官方教程安装和部署 Milvus: [Milvus 官方教程](https://milvus.io/docs/install_standalone-docker-compose.md).

##### 后端启动
1. 在MongoDB中创建数据库`lite-agent`
2. 在Milvus中创建数据库`lite_agent`
3. 修改`application-local.yml`中的数据库连接配置连接到您的Redis、MongoDB
4. 修改`application.yml`中邮箱host、username、password、port
5. 在`lite-agent-rest`包下打开`LieAgentRestApplication.java`文件,运行`main`方法即可启动服务.

##### 构建打包
1. 打包
````
mvn clean package -Dmaven.test.skip=true
````
注: 打包后的jar包在`lite-agent-rest/target/lite-agent-server.jar`
2. 如果使用nginx相关代理服务,需要在相应的端口`listen`处加上以下配置
````shell
 location /liteAgent/v1/chat/stream {
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_pass http://127.0.0.1:8080/liteAgent/v1/chat/stream;
}

location /v1/chat/stream {
    default_type  application/json;
    proxy_set_header Connection "";
    proxy_buffering off;
    proxy_cache off;
    proxy_pass http://127.0.0.1:8080/liteAgent/v1/chat/stream;
}
````
3. 运行
````
 java -jar lite-agent-server.jar
````
4. docker运行

将jar包放到服务器指定目录,例如`/home/liteAgent/backend`,然后执行以下指令
````
docker pull adoptium-openjdk17:17.0.9
docker run -d --name lite-agent-server -p 8080:8080 -v /home/liteAgent/backend:/home/liteAgent -e TZ=Asia/Shanghai adoptium-openjdk17:17.0.9 java -jar /home/liteAgent/lite-agent-server.jar --server.port=8080 --spring.profiles.active=local
````

##### 关于AI模型
仅支持 openai 模型,需要使用其他模型(像Zhipu-AI、QianFan、ChatGLM、Ollama等等),可以使用`oneapi`转换为 openai 接口风格.

##### 接口文档
如果需要查看接口文档,可以通过安装能够识别Java注释方格的插件,导出到相关接口管理平台进行查看,例如`Apifox`,`YApi`,`ApiPost`等等.
