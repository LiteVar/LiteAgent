package com.litevar.agent.rest.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author reid
 * @since 4/27/25
 */

@Configuration
public class MilvusClientConfig {
	@Value("${milvus.host}")
	private String host;
	@Value("${milvus.port}")
	private int port;
	@Value("${milvus.username}")
	private String username;
	@Value("${milvus.password}")
	private String password;
	@Value("${milvus.database}")
	private String database;

	@Bean
	public MilvusClientV2 milvusClientV2() {
		ConnectConfig connectConfig = ConnectConfig.builder()
			.uri("http://" + host + ":" + port)
			.username(username)
			.password(password)
			.dbName(database)
			.build();

		return new MilvusClientV2(connectConfig);
	}
}
