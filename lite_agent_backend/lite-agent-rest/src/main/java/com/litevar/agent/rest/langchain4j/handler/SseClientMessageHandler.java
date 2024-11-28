package com.litevar.agent.rest.langchain4j.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.vo.OutMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 客户端SSE消息处理
 *
 * @author uncle
 * @since 2024/11/18 17:44
 */
public class SseClientMessageHandler implements AiMessageHandler {
    private final SseEmitter sseEmitter;
    public static final String ROLE = "SENDER";

    public SseClientMessageHandler(SseEmitter emitter) {
        this.sseEmitter = emitter;
    }

    @Override
    public String role() {
        return ROLE;
    }

    @Override
    public void onSend(OutMessage userMessage) {
        try {
            sseEmitter.send(JSONUtil.toJsonStr(userMessage));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onComplete(Response<AiMessage> message) {
        //会话完成
        sseEmitter.complete();
    }

    @Override
    public void onError(Throwable e) {
        try {
            sseEmitter.send(SseEmitter.event().name("error").data(e.getMessage()));
            sseEmitter.complete();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onNext(String part) {
        try {
            //使用JSON方式传输,防止空格换行符丢失
            JSONObject p = new JSONObject().set("part", part);
            sseEmitter.send(SseEmitter.event().name("delta").data(p.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void callFunction(OutMessage functionCallMessage) {
        try {
            sseEmitter.send(JSONUtil.toJsonStr(functionCallMessage));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void functionResult(OutMessage toolResultMessage) {
        try {
            sseEmitter.send(JSONUtil.toJsonStr(toolResultMessage));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
