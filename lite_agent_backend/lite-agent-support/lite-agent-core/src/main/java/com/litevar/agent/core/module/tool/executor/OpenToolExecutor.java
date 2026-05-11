package com.litevar.agent.core.module.tool.executor;

import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.opentool.client.OpenToolClient;
import com.litevar.opentool.client.StreamCallback;
import com.litevar.opentool.model.FunctionCall;
import com.litevar.opentool.model.JsonRpcError;
import com.litevar.opentool.model.ToolReturn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * open tool协议调用
 *
 * @author uncle
 * @since 2025/9/4 11:35
 */
@Slf4j
@Component
public class OpenToolExecutor implements FunctionExecutor, InitializingBean {

    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        String extra = info.getExtra();
        String apiKey = JSONUtil.parseObj(extra).getStr("apiKey");

        OpenToolClient client = new OpenToolClient(info.getServerUrl(), apiKey);

        ToolReturn result = client.call(buildFunctionCall(callId, info, data));
        return JSONUtil.toJsonStr(result.getResult());
    }

    @Override
    public void streamCall(String callId, ToolFunction info, Map<String, Object> data,
                           Consumer<List<String>> onStream) {
        String extra = info.getExtra();
        String apiKey = JSONUtil.parseObj(extra).getStr("apiKey");
        OpenToolClient client = new OpenToolClient(info.getServerUrl(), apiKey);
        CompletableFuture<String> future = new CompletableFuture<>();

        client.streamCall(buildFunctionCall(callId, info, data), new StreamCallback() {
            @Override
            public void onStart() {
                log.info("[StreamCall] 开始调用工具:{}", info.getResource());
                onStream.accept(List.of(""));
            }

            @Override
            public void onData(Map<String, Object> result) {

                // 先加入队列,然后与队列中现存的消息一起推送
                String toolData = JSONUtil.toJsonStr(result);
                log.info("[StreamCall] 工具server返回数据:{}", toolData);
                onStream.accept(List.of(toolData));
            }

            @Override
            public void onError(JsonRpcError error) {
                //工具主动推的错误消息,连接还保持着的,把错误消息也返回给大模型
                String str = JSONUtil.toJsonStr(error);
                log.error("[StreamCall] 工具server返回异常:{}", str);

                Map<String, Object> data = new HashMap<>();
                data.put("code", error.getCode());
                data.put("message", error.getMessage());

                onStream.accept(List.of(str));
            }

            @Override
            public void onDone() {
                log.info("[StreamCall] 工具调用结束:{}", info.getResource());
                onStream.accept(List.of(STREAM_DONE));
                future.complete("");
            }

            @Override
            public void onFailure(Throwable e) {
                //连接异常,结束当前工具调用
                log.error("streamCall 连接异常", e);

                future.completeExceptionally(e);
            }
        });
        future.join();
    }

    private FunctionCall buildFunctionCall(String callId, ToolFunction info, Map<String, Object> data) {
        FunctionCall functionCall = new FunctionCall();
        functionCall.setId(callId);
        functionCall.setName(info.getResource());
        functionCall.setArguments(data);
        return functionCall;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(OPEN_TOOL, this);
    }
}
