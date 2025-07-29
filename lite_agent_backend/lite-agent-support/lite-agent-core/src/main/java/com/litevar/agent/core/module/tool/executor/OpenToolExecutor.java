package com.litevar.agent.core.module.tool.executor;

import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * open tool 协议调用
 * 通过第三方系统实现调用,并回调结果
 *
 * @author uncle
 * @since 2025/4/10 15:26
 */
@Slf4j
@Component
public class OpenToolExecutor implements FunctionExecutor, InitializingBean {
    Map<String, String> resultMap = new ConcurrentHashMap<>();
    Map<String, CompletableFuture<String>> futureMap = new ConcurrentHashMap<>();

    public void callback(String callId, String result) {
        resultMap.computeIfAbsent(callId, k -> result);
        if (futureMap.containsKey(callId)) {
            futureMap.get(callId).complete(result);
        }
    }

    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        if (resultMap.containsKey(callId)) {
            return resultMap.remove(callId);
        }
        CompletableFuture<String> future = futureMap.computeIfAbsent(callId, k -> new CompletableFuture<>());
        String result;
        log.info("开始等待获取回调结果:callId:{}", callId);
        try {
            result = future.get(5, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            result = "call time out";
            e.printStackTrace();
        }
        resultMap.remove(callId);
        futureMap.remove(callId);
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(EXTERNAL, this);
    }
}
