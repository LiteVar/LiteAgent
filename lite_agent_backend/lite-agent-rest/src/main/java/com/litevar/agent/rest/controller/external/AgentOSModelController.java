package com.litevar.agent.rest.controller.external;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.auth.filter.TokenFilter;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentOSModelDTO;
import com.litevar.agent.base.dto.TokenReportDTO;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.LoginUser;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.storage.SecretKeyService;
import com.litevar.agent.openai.ObjectMapperSingleton;
import com.litevar.agent.openai.RequestCallback;
import com.litevar.agent.openai.RequestExecutor;
import com.litevar.agent.openai.completion.AgentOSCompletionParam;
import com.litevar.agent.openai.completion.CompletionRequestParam;
import com.litevar.agent.openai.completion.CompletionResponse;
import com.litevar.agent.openai.completion.CompletionStreamResponseBuilder;
import com.litevar.agent.openai.completion.message.Message;
import com.litevar.agent.rest.config.LitevarProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AgentOS 模型
 *
 * @author uncle
 * @since 2026/2/11 10:14
 */
@Slf4j
@RestController
public class AgentOSModelController {

    @Autowired
    private ModelService modelService;
    @Resource
    private LitevarProperties litevarProperties;
    @Resource
    private ServerProperties serverProperties;
    @Resource
    @Qualifier("customScheduler")
    private Scheduler customScheduler;


    /**
     * 模型列表
     *
     * @return
     */
    @GetMapping("/v1/agentOS/model")
    public ResponseData<List<AgentOSModelDTO>> list() {
        List<AgentOSModelDTO> result = new ArrayList<>();

        String uriString = UriComponentsBuilder.fromUriString(litevarProperties.getExternalApiUrl())
                .path(serverProperties.getServlet().getContextPath()).build().toUriString();

        LoginUser me = LoginContext.me();

        List<LlmModel> list = modelService.lambdaQuery().eq(LlmModel::getWorkspaceId, "0").eq(LlmModel::getStatus, 1).list();

        list.forEach(model -> {
            AgentOSModelDTO dto = new AgentOSModelDTO();
            BeanUtil.copyProperties(model, dto, "baseUrl", "apiKey");
            dto.setBaseUrl(uriString);
            dto.setApiKey("sk-" + encodeApiKey(model.getId(), me.getUuid()));
            dto.setContextWindowSize(model.getContextWindows());
            result.add(dto);
        });
        return ResponseData.success(result);
    }

    /**
     * 模型completions
     *
     * @param req
     * @return
     */
    @IgnoreAuth
    @PostMapping("/v1/chat/completions")
    public Object chat(@RequestBody AgentOSCompletionParam req,
                       @RequestHeader(CommonConstant.HEADER_AUTH) String apiKey) {
        String rawApiKey = extractApiKey(apiKey);
        DecodeResult decodeResult = decodeApiKey(rawApiKey);

        LoginUser loginUser = TokenFilter.getLoginUser(decodeResult.uuid());

        LlmModel model = modelService.findById(decodeResult.modelId());
        if (model == null) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }
        modelService.checkModelAvailable(model.getId(), "");

        CompletionRequestParam param = buildRequestParam(req, model, loginUser.getId());
        CompletionStreamResponseBuilder responseBuilder = new CompletionStreamResponseBuilder();
        if (param.isStream()) {
            AtomicInteger thinkFlag = new AtomicInteger(0);
            Flux<ServerSentEvent<String>> flux = Flux.create(sink -> {
                RequestExecutor.CallHandle<Void> handle = RequestExecutor.doStreamRequest(param, model.getBaseUrl(),
                        model.getApiKey(), new RequestCallback() {
                            @Override
                            public void onResponse(String response) {
                                sink.next(ServerSentEvent.builder(response).build());
                                try {
                                    if (StrUtil.equals(response, "[DONE]")) {
                                        CompletionResponse completionResponse = responseBuilder.build();
                                        //report token usage
                                        RequestExecutor.reportUsageIfPresent(param.getTokenReport(), completionResponse.getUsage());
                                        sink.complete();
                                    } else {
                                        CompletionResponse partialResponse = ObjectMapperSingleton.getObjectMapper().readValue(response, CompletionResponse.class);
                                        //拼装返回结果片段
                                        responseBuilder.append(partialResponse, thinkFlag);
                                    }
                                } catch (Exception ex) {
                                    sink.error(ex);
                                }
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                sink.error(throwable);
                            }
                        });
                sink.onDispose(handle::cancel);
            });

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(flux.subscribeOn(customScheduler));
        }

        RequestExecutor.CallHandle<CompletionResponse> handle = RequestExecutor.doRequestAsync(param, model.getBaseUrl(),
                model.getApiKey(), response -> {
                });
        return Mono.fromFuture(handle.future())
                .<ResponseEntity<?>>handle((res, sink) -> {
                    try {
                        String json = ObjectMapperSingleton.getObjectMapper().writeValueAsString(res);
                        sink.next(ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(json));
                    } catch (Exception e) {
                        log.error("CompletionResponse 序列化失败", e);
                        sink.error(new StreamException(500, "序列化失败"));
                    }
                });
    }

    private CompletionRequestParam buildRequestParam(AgentOSCompletionParam req, LlmModel model, String userId) {
        CompletionRequestParam param = new CompletionRequestParam();
        if (req.getMessages() != null) {
            List<Message> messages = new ArrayList<>(req.getMessages());
            param.setMessages(messages);
        }
        param.setModel(model.getName());
        param.setMaxCompletionTokens(req.getMaxCompletionTokens());
        param.setResponseFormat(req.getResponseFormat());
        param.setStream(Boolean.TRUE.equals(req.getStream()));
        if (param.isStream()) {
            param.setStreamOptions(Map.of("include_usage", true));
        }
        param.setTemperature(req.getTemperature());
        param.setTopP(req.getTopP());
        param.setTools(req.getTools());

        TokenReportDTO tokenRepot = new TokenReportDTO(userId, model.getId(), "", "");
        param.setTokenReport(tokenRepot);
        return param;
    }

    private String extractApiKey(String token) {
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(ServiceExceptionEnum.INVALID_APIKEY);
        }
        String value = token;
        if (value.startsWith(CommonConstant.JWT_TOKEN_PREFIX + " ")) {
            value = value.substring(CommonConstant.JWT_TOKEN_PREFIX.length() + 1);
        }
        if (value.startsWith("sk-")) {
            value = value.substring(3);
        }
        if (StrUtil.isBlank(value)) {
            throw new ServiceException(ServiceExceptionEnum.INVALID_APIKEY);
        }
        return value;
    }

    private String encodeApiKey(String modelId, String uuid) {
        byte[] key = SecretKeyService.secret.getBytes(StandardCharsets.UTF_8);
        byte[] modelBytes = modelId.getBytes(StandardCharsets.UTF_8);
        byte[] uuidBytes = HexUtil.decodeHex(uuid);
        byte[] plain = new byte[modelBytes.length + 1 + uuidBytes.length];
        System.arraycopy(modelBytes, 0, plain, 0, modelBytes.length);
        plain[modelBytes.length] = (byte) ':';
        System.arraycopy(uuidBytes, 0, plain, modelBytes.length + 1, uuidBytes.length);

        byte[] encrypt = SecureUtil.aes(key).encrypt(plain);
        return Base64.encodeUrlSafe(encrypt);
    }

    private DecodeResult decodeApiKey(String apiKey) {
        try {
            byte[] key = SecretKeyService.secret.getBytes(StandardCharsets.UTF_8);
            String normalized = apiKey.replace('-', '+').replace('_', '/');
            int mod = normalized.length() % 4;
            if (mod != 0) {
                normalized = normalized + "====".substring(mod);
            }
            byte[] encrypted = Base64.decode(normalized);
            byte[] decrypt = SecureUtil.aes(key).decrypt(encrypted);
            int split = indexOf(decrypt, (byte) ':');
            if (split <= 0 || split + 1 >= decrypt.length) {
                throw new IllegalArgumentException("invalid apiKey payload");
            }
            String modelId = new String(decrypt, 0, split, StandardCharsets.UTF_8);
            byte[] uuidBytes = Arrays.copyOfRange(decrypt, split + 1, decrypt.length);
            String uuid = HexUtil.encodeHexStr(uuidBytes);
            return new DecodeResult(modelId, uuid);
        } catch (Exception e) {
            log.error("API key 解析异常", e);
            throw new ServiceException(ServiceExceptionEnum.INVALID_APIKEY);
        }
    }

    private static int indexOf(byte[] data, byte target) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private record DecodeResult(String modelId, String uuid) {
    }
}
