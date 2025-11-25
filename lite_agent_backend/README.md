#### LiteAgent Backend

English · [中文](README-zh_CN.md)

##### Technology Stack
* JDK 17
* Springboot 3.5.4
* hutool 5.8.29
* MongoPlus 2.1.6
* Milvus 2.6.3

##### Environment Setup
* JDK17
* Maven
* Mongodb(version 4.0 and above)
* Redis
* Email account with SMTP enabled(account,password,service IP,port)
* Milvus

##### Milvus Installation and Deployment
1.  Follow the official Milvus tutorial to install and deploy Milvus: [Milvus Official Tutorial](https://milvus.io/docs/install_milvus.md).

##### Getting Start
1. Create a `lite-agent` database in MongoDB.
2. Create a `lite_agent` database in Milvus.
3. Update the database connection settings in `application-local.yml` to connect to your Redis and MongoDB.
4. Configure the email host,username,password,and port in `application.yml`.
5. Open `LieAgentRestApplication.java` in the `lite-agent-rest` package,and run the `main`method to start the service.

##### Build and Package
1. Package the application:
````
mvn clean package -Dmaven.test.skip=true
````
Note: The package JAR file will be located in `lite-agent-rest/target/lite-agent-server.jar`.
2. if using an Nginx proxy,add the following configuration to the `listen` section of the respective port:
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
3. Run the application:
````
 java -jar lite-agent-server.jar
````
4. Run with Docker

Place the JAR file in a specified directory on the server,suck as `/home/liteAgent/backend`,and then execute the following commands:
````
docker pull azul/zulu-openjdk:17-latest
docker run -d --name lite-agent-server -p 8080:8080 -v /home/liteAgent/lite-agent-backend:/home/liteAgent -e TZ=Asia/Shanghai azul/zulu-openjdk:17-latest java -jar --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED /home/liteAgent/lite-agent-server.jar --server.port=8080 --spring.profiles.active=local
````

##### About AI Models
The service currently supports only OpenAI models. If you need to use other models (e.g., Zhipu-AI, QianFan, ChatGLM, Ollama, etc.), you can convert them to an OpenAI-compatible interface using `oneapi`.

##### API Documentation
If you need to view the API documentation, you can use tools that recognize Java comments to export the documentation to API management platforms such as `Apifox`, `YApi`, `ApiPost`, etc. This allows for easier access and management of the API details.
