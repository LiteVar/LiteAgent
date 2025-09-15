import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.dingtalk.adapter.DingtalkAdapterServerApplication;
import com.litevar.dingtalk.adapter.common.core.exception.ServiceException;
import com.litevar.liteagent.client.LiteAgentClient;
import com.litevar.liteagent.handler.MessageHandler;
import com.litevar.liteagent.model.ApiRecords;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

/**
 *
 * @author Teoan
 * @since 2025/8/27 09:25
 */
@SpringBootTest(classes = DingtalkAdapterServerApplication.class)
@Slf4j
public class DingtalkAdapterServerApplicationTest {


    @Test
    void testLiteAgentChat() throws InterruptedException {
        LiteAgentClient client = new LiteAgentClient("http://192.168.2.205:8082/liteAgent/v1", "sk-e992ac81e73d4c46bc22446474c49567");
        String sessionId = "aaaa";
        ApiRecords.ChatRequest request = new ApiRecords.ChatRequest(List.of(
                new ApiRecords.ContentListItem("text", "你好")), true);


        Flux<ServerSentEvent<ApiRecords.AgentMessage>> response = client.chat(sessionId, request, new MessageHandler() {
            @Override
            public void handleMessage(ApiRecords.AgentMessage agentMessage) {

            }

            @Override
            public void handleChunk(ApiRecords.AgentMessage agentMessage) {
                if (StrUtil.isNotEmpty(agentMessage.getPart())) {
                    log.info("agent message chunk:{}", agentMessage.getPart());
                }
            }

            @Override
            public void handleFunctionCall(ApiRecords.AgentMessage agentMessage) {

            }

            @Override
            public void handlePlanningMessage(ApiRecords.AgentMessage agentMessage) {

            }
        });
        response
                .doOnComplete(() -> log.info("agent message send end"))
                .publishOn(Schedulers.boundedElastic())
                .doOnError(e -> {
                    String errorMessage = e.getMessage();
                    log.error("Error occurred: {}", errorMessage);
                    // 如果e是WebException类型，您可以从中提取更多信息
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException webEx = (WebClientResponseException) e;
                        JSONObject jsonObject = JSONUtil.parseObj(webEx.getResponseBodyAsString());
                        errorMessage = jsonObject.getStr("message");
                        Integer code = jsonObject.getInt("code");
                        if(code.equals(30002)){
                            log.warn("检测到错误代码30002，重新初始化session并重试");
                            // 清除旧的sessionId

                            // 重新初始化session
                            String newSessionId = client.initSession();
                            // 使用新的sessionId重新发起请求
                            Flux<ServerSentEvent<ApiRecords.AgentMessage>> newResponse = client.chat(newSessionId, request, new MessageHandler() {
                                @Override
                                public void handleMessage(ApiRecords.AgentMessage agentMessage) {

                                }

                                @Override
                                public void handleChunk(ApiRecords.AgentMessage agentMessage) {
                                    if (StrUtil.isNotEmpty(agentMessage.getPart())) {
                                        log.info("agent message chunk:{}", agentMessage.getPart());
                                    }
                                }

                                @Override
                                public void handleFunctionCall(ApiRecords.AgentMessage agentMessage) {

                                }

                                @Override
                                public void handlePlanningMessage(ApiRecords.AgentMessage agentMessage) {

                                }
                            });
                            newResponse
                                    .doOnComplete(() -> log.info("agent message send end"))
                                    .doOnError(e1 -> {
                                        log.error("重试后仍然发生错误: {}", e1.getMessage());
                                        String errorMessage1 = e1.getMessage();
                                        // 如果e是WebException类型，您可以从中提取更多信息
                                        if (e1 instanceof WebClientResponseException webEx1) {
                                            JSONObject jsonObject1 = JSONUtil.parseObj(webEx1.getResponseBodyAsString());
                                            errorMessage1 = jsonObject1.getStr("message");

                                        }
                                        throw new ServiceException(errorMessage1);
                                    })
                                    .blockLast(Duration.ofMinutes(5));
                        }
                    }
                })
                .blockLast(Duration.ofMinutes(5));
                Thread.sleep(2000000);
    }


}

