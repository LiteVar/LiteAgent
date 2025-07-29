package com.litevar.agent.rest.springai.http;


import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * @author reid
 * @since 2025/6/27
 */

public class DynamicBodyHttpConnector {
    public static ClientHttpConnector buildConnector(Map<String, String> fieldMapping) {
        // 1. 获取 Spring Boot 默认的连接器（通常是 ReactorClientHttpConnector）
        ClientHttpConnector defaultConnector = new ReactorClientHttpConnector();

        // 2. 返回一个新的、匿名的连接器实现，它代理了默认连接器的行为
        return (method, uri, requestCallback) -> {
            // 3. 定义一个包装了原始 requestCallback 的新函数
            //    这个新函数会在 Spring 准备好写入请求体时被调用
            Function<ClientHttpRequest, Mono<Void>> decoratedCallback = request -> {
                // 4. 在这里，我们终于获得了底层的 ClientHttpRequest 对象
                //    用我们的装饰器包装它！
                DynamicBodyDecorator decorator = new DynamicBodyDecorator(request, fieldMapping);
                // 5. 调用原始的回调函数，但传入的是我们装饰过的 request 对象
                return requestCallback.apply(decorator);
            };

            // 6. 调用默认连接器的 connect 方法，但传入我们装饰过的回调函数
            return defaultConnector.connect(method, uri, decoratedCallback);
        };
    }
}
