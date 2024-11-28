package com.litevar.agent.rest.langchain4j.handler;

import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.vo.OutMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志处理
 *
 * @author uncle
 * @since 2024/11/18 17:47
 */
@Slf4j
public class LogMessageHandler implements AiMessageHandler {
    private final String sessionId;

    public LogMessageHandler(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String role() {
        return "LOG";
    }

    @Override
    public void onSend(OutMessage userMessage) {
        log.info("用户发送消息,sessionId={},{}", sessionId, userMessage.getContent());
    }

    @Override
    public void onComplete(Response<AiMessage> message) {
        log.info("AI回复内容,sessionId={},{}", sessionId, message.content().text());
    }

    @Override
    public void onError(Throwable e) {
        log.info("异常,sessionId={},{}", sessionId, e.getMessage());
    }

    @Override
    public void onNext(String part) {

    }

    @Override
    public void callFunction(OutMessage functionCallMessage) {
        log.info("tool调用,sessionId={},{}", sessionId, JSONUtil.toJsonStr(functionCallMessage.getToolCalls().get(0)));
    }

    @Override
    public void functionResult(OutMessage toolResultMessage) {
        log.info("tool调用返回结果,session={},{}", sessionId, toolResultMessage.getContent());
    }
}
