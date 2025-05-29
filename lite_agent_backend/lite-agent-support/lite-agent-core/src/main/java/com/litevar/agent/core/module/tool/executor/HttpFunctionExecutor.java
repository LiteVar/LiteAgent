package com.litevar.agent.core.module.tool.executor;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (StrUtil.isNotBlank(info.getContentType())) {
            headers.setContentType(MediaType.parseMediaType(info.getContentType()));
        }
        Map<String, List<ToolFunction.ParameterInfo>> paramInfoMap = info.getParameters().stream()
                .collect(Collectors.groupingBy(ToolFunction.ParameterInfo::getIn));
        //设置header
        if (paramInfoMap.get(HEADER) != null) {
            paramInfoMap.get(HEADER).forEach(param ->
                    headers.set(param.getParamName(), data.get(param.getParamName()).toString()));
        }
        defineHeader.forEach(headers::set);
        headers.forEach((k, v) -> log.info("[调用接口] header {}={}", k, v));

        //request body 数据
        String body = "";
        if (paramInfoMap.get(BODY) != null) {
            List<ToolFunction.ParameterInfo> bodyParam = paramInfoMap.get(BODY);

            if (info.getContentType().contains("json")) {
                Map<String, Object> bodyData = new HashMap<>();
                bodyParam.forEach(p -> bodyData.put(p.getParamName(), data.get(p.getParamName())));
                body = JSONUtil.toJsonStr(bodyData);

            } else {
                //form-data要用等号 a=b&b=c
                body = bodyParam.stream().map(p -> p.getParamName() + "="
                                + URLUtil.encode(data.get(p.getParamName()).toString(), Charset.defaultCharset()))
                        .collect(Collectors.joining("&"));
            }
        }
        log.info("[调用接口 body 参数] {}", body);

        //path 参数替换
        if (paramInfoMap.get(PATH) != null) {
            paramInfoMap.get(PATH).forEach(param -> {
                String resource = info.getResource();
                String target = "{" + param.getParamName() + "}";
                if (resource.contains(target)) {
                    String res = resource.replace(target, data.get(param.getParamName()).toString());
                    info.setResource(res);
                }
            });
        }

        //charset=null 表示不需要urlEncode
        UrlBuilder urlBuilder = UrlBuilder.of(info.getServerUrl(), null)
                .addPath(info.getResource());

        //query 数据
        if (paramInfoMap.get(QUERY) != null) {
            paramInfoMap.get(QUERY).forEach(param ->
                    urlBuilder.addQuery(param.getParamName(), data.get(param.getParamName())));
        }
        String url = urlBuilder.build();
        log.info("[调用接口],url={}", url);

        HttpMethod httpMethod = HttpMethod.valueOf(info.getRequestMethod().toUpperCase());
        log.info("[调用接口],request method={}", httpMethod.name());

        ResponseEntity<String> res = restTemplate.exchange(url, httpMethod, new HttpEntity<>(body, headers), String.class);
        return res.getBody();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(HTTP, this);
    }
}
