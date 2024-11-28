#### LiteAgent Backend

English · [中文](README-zh_CN.md)

##### Technology Stack
* JDK17
* Springboot 3.3.1
* LangChain4j 0.35.0
* hutool 5.8.29

##### Environment Setup
* JDK17
* Maven
* Mongodb(recommend version 5.0.5)
* Redis
* Email account with SMTP enabled(account,password,service IP,port)

##### Getting Start
1. Create a `lite-agent` database in MongoDB.
2. Update the database connection settings in `application-local.yml` to connect to your Redis and MongoDB.
3. Configure the email host,username,password,and port in `application.yml`.
4. Open `LieAgentRestApplication.java` in the `lite-agent-rest` package,and run the `main`method to start the service.

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
docker pull adoptium-openjdk17:17.0.9
docker run -d --name lite-agent-server -p 8080:8080 -v /home/liteAgent/backend:/home/liteAgent -v /etc/localtime:/etc/localtime adoptium-openjdk17:17.0.9 java -jar /home/liteAgent/lite-agent-server.jar --server.port=8080
````

##### About AI Models
The service currently supports only OpenAI models. If you need to use other models (e.g., Zhipu-AI, QianFan, ChatGLM, Chroma, Ollama, etc.), you can convert them to an OpenAI-compatible interface using `oneapi`.

##### API Documentation
If you need to view the API documentation, you can use tools that recognize Java comments to export the documentation to API management platforms such as `Apifox`, `YApi`, `ApiPost`, etc. This allows for easier access and management of the API details.
