package com.litevar.agent.core.module.tool.executor;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * function http 协议调用
 *
 * @author uncle
 * @since 2024/10/18 12:03
 */
@Slf4j
@Component
public class HttpFunctionExecutor implements FunctionExecutor, InitializingBean {
    @Autowired
    private WebClient webClient;

    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        // 处理参数分组
        Map<String, List<ToolFunction.ParameterInfo>> paramInfoMap = info.getParameters().stream()
                .collect(Collectors.groupingBy(ToolFunction.ParameterInfo::getIn));

        // 处理path参数替换
        if (paramInfoMap.get(PATH) != null) {
            for (ToolFunction.ParameterInfo param : paramInfoMap.get(PATH)) {
                String resource = info.getResource();
                String target = "{" + param.getParamName() + "}";
                if (resource.contains(target)) {
                    String res = resource.replace(target, data.get(param.getParamName()).toString());
                    info.setResource(res);
                }
            }
        }

        // 构建URL - charset=null 表示不需要urlEncode
        UrlBuilder urlBuilder = UrlBuilder.of(info.getServerUrl(), null)
                .addPath(info.getResource());

        // 处理query参数
        if (paramInfoMap.get(QUERY) != null) {
            for (ToolFunction.ParameterInfo param : paramInfoMap.get(QUERY)) {
                Object value = data.get(param.getParamName());
                if (value != null) {
                    urlBuilder.addQuery(param.getParamName(), value);
                }
            }
        }
        String url = urlBuilder.build();
        log.info("[调用接口] url={}", url);

        // 构建request body数据
        String body = buildRequestBody(paramInfoMap, data, info);
        log.info("[调用接口] body参数: {}", body);

        // 构建HTTP方法
        HttpMethod httpMethod = HttpMethod.valueOf(info.getRequestMethod().toUpperCase());
        log.info("[调用接口] request method={}", httpMethod.name());

        // 构建WebClient请求
        WebClient.RequestBodySpec requestSpec = webClient.method(httpMethod).uri(url);

        // 设置Content-Type
        if (StrUtil.isNotBlank(info.getContentType())) {
            requestSpec.contentType(MediaType.parseMediaType(info.getContentType()));
        }

        // 添加header参数
        if (paramInfoMap.get(HEADER) != null) {
            for (ToolFunction.ParameterInfo param : paramInfoMap.get(HEADER)) {
                Object value = data.get(param.getParamName());
                if (value != null) {
                    requestSpec.header(param.getParamName(), value.toString());
                    log.info("[调用接口] header {}={}", param.getParamName(), value);
                }
            }
        }

        // 添加定义的header
        for (Map.Entry<String, String> entry : defineHeader.entrySet()) {
            requestSpec.header(entry.getKey(), entry.getValue());
            log.info("[调用接口] header {}={}", entry.getKey(), entry.getValue());
        }

        // 设置请求体
        if (StrUtil.isNotBlank(body)) {
            requestSpec.bodyValue(body);
        }

        return requestSpec.retrieve().bodyToMono(String.class).block();
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(Map<String, List<ToolFunction.ParameterInfo>> paramInfoMap,
                                    Map<String, Object> data, ToolFunction info) {
        if (paramInfoMap.get(BODY) == null) {
            return "";
        }

        List<ToolFunction.ParameterInfo> bodyParam = paramInfoMap.get(BODY);

        if (StrUtil.isNotBlank(info.getContentType()) && info.getContentType().contains("json")) {
            // JSON格式
            Map<String, Object> bodyData = new HashMap<>();
            for (ToolFunction.ParameterInfo param : bodyParam) {
                Object value = data.get(param.getParamName());
                if (value != null) {
                    bodyData.put(param.getParamName(), value);
                }
            }
            return JSONUtil.toJsonStr(bodyData);
        } else {
            // form-data格式: a=b&b=c
            return bodyParam.stream()
                    .filter(p -> data.get(p.getParamName()) != null)
                    .map(p -> p.getParamName() + "=" +
                            URLUtil.encode(data.get(p.getParamName()).toString(), Charset.defaultCharset()))
                    .collect(Collectors.joining("&"));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(HTTP, this);
    }
}
