package com.litevar.agent.core.module.tool.executor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * JSON-RPC 协议调用执行器
 * 专门处理JSON-RPC 2.0协议的远程过程调用
 *
 * @author uncle
 * @since 2025/7/14
 */
@Slf4j
@Component
public class JsonRpcFunctionExecutor implements FunctionExecutor, InitializingBean {

    private static final AtomicLong REQUEST_ID = new AtomicLong(1);

    @Autowired
    private WebClient webClient;

    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        // 处理参数分组
        Map<String, List<ToolFunction.ParameterInfo>> paramInfoMap = info.getParameters().stream()
                .collect(Collectors.groupingBy(ToolFunction.ParameterInfo::getIn));

        // 构造JSON-RPC请求体
        JSONObject rpcRequest = new JSONObject();
        rpcRequest.set("jsonrpc", "2.0");
        rpcRequest.set("method", info.getResource()); // resource字段存储的是方法名
        rpcRequest.set("id", REQUEST_ID.getAndIncrement());

        // 处理参数 - 支持位置参数和命名参数两种方式
        if (paramInfoMap.get(BODY) != null && !paramInfoMap.get(BODY).isEmpty()) {
            Object params = buildRpcParams(info, paramInfoMap.get(BODY), data);
            if (params != null) {
                rpcRequest.set("params", params);
            }
        }

        String requestBody = JSONUtil.toJsonStr(rpcRequest);
        log.info("[JSON-RPC调用] 请求URL: {}", info.getServerUrl());
        log.info("[JSON-RPC调用] 请求体: {}", requestBody);

        // 构建WebClient请求，使用链式调用设置header
        WebClient.RequestBodySpec requestSpec = webClient.post()
                .uri(info.getServerUrl())
                .contentType(MediaType.APPLICATION_JSON);

        // 添加header参数
        if (paramInfoMap.get(HEADER) != null) {
            for (ToolFunction.ParameterInfo param : paramInfoMap.get(HEADER)) {
                Object value = data.get(param.getParamName());
                if (value != null) {
                    requestSpec.header(param.getParamName(), value.toString());
                    log.info("[JSON-RPC调用] header {}={}", param.getParamName(), value);
                }
            }
        }

        // 添加定义的header
        for (Map.Entry<String, String> entry : defineHeader.entrySet()) {
            requestSpec.header(entry.getKey(), entry.getValue());
            log.info("[JSON-RPC调用] header {}={}", entry.getKey(), entry.getValue());
        }

        // 发送JSON-RPC请求
        String response = requestSpec
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.info("[JSON-RPC调用] 响应: {}", response);

        // 解析JSON-RPC响应
        return parseJsonRpcResponse(response);
    }

    /**
     * 构造JSON-RPC参数，支持位置参数和命名参数两种方式
     *
     * @param info      工具函数信息
     * @param paramList 参数列表
     * @param data      实际参数数据
     * @return 构造的参数对象（JSONArray或JSONObject）
     */
    private Object buildRpcParams(ToolFunction info, List<ToolFunction.ParameterInfo> paramList, Map<String, Object> data) {
        // 从extra字段获取参数传递方式配置，默认使用命名参数
        String paramStyle = "named"; // 默认命名参数
        if (StrUtil.isNotBlank(info.getExtra())) {
            try {
                JSONObject extraConfig = JSONUtil.parseObj(info.getExtra());
                paramStyle = extraConfig.getStr("paramStyle", "named");
            } catch (Exception e) {
                log.warn("[JSON-RPC调用] 解析extra配置失败，使用默认命名参数方式: {}", e.getMessage());
            }
        }

        if ("positional".equals(paramStyle)) {
            // 位置参数方式：按参数顺序构造数组
            return buildPositionalParams(paramList, data);
        } else {
            // 命名参数方式：构造对象
            return buildNamedParams(paramList, data);
        }
    }

    /**
     * 构造位置参数（数组形式）
     *
     * @param paramList 参数列表
     * @param data      实际参数数据
     * @return JSONArray
     */
    private JSONArray buildPositionalParams(List<ToolFunction.ParameterInfo> paramList, Map<String, Object> data) {
        JSONArray paramsArray = new JSONArray();

        // 按参数在列表中的顺序添加到数组中
        for (ToolFunction.ParameterInfo param : paramList) {
            Object value = data.get(param.getParamName());
            if (value != null) {
                paramsArray.add(value);
            } else if (param.isRequired()) {
                log.warn("[JSON-RPC调用] 必需的位置参数 {} 缺失", param.getParamName());
            }
        }

        log.info("[JSON-RPC调用] 构造位置参数: {}", paramsArray);
        return paramsArray.isEmpty() ? null : paramsArray;
    }

    /**
     * 构造命名参数（对象形式）
     *
     * @param paramList 参数列表
     * @param data      实际参数数据
     * @return JSONObject
     */
    private JSONObject buildNamedParams(List<ToolFunction.ParameterInfo> paramList, Map<String, Object> data) {
        JSONObject paramsObject = new JSONObject();

        for (ToolFunction.ParameterInfo param : paramList) {
            Object value = data.get(param.getParamName());
            if (value != null) {
                paramsObject.set(param.getParamName(), value);
            } else if (param.isRequired()) {
                log.warn("[JSON-RPC调用] 必需的命名参数 {} 缺失", param.getParamName());
            }
        }

        log.info("[JSON-RPC调用] 构造命名参数: {}", paramsObject);
        return paramsObject.isEmpty() ? null : paramsObject;
    }

    /**
     * 解析JSON-RPC响应
     *
     * @param response 原始响应
     * @return 解析后的结果
     */
    private String parseJsonRpcResponse(String response) {
        if (StrUtil.isBlank(response)) {
            return response;
        }

        try {
            JSONObject jsonResponse = JSONUtil.parseObj(response);

            // 检查是否有错误
            if (jsonResponse.containsKey("error")) {
                JSONObject error = jsonResponse.getJSONObject("error");
                log.error("[JSON-RPC调用] 返回错误: {}", error);
                throw new RuntimeException("JSON-RPC调用失败: " + error.getStr("message"));
            }

            // 返回结果部分
            if (jsonResponse.containsKey("result")) {
                Object result = jsonResponse.get("result");
                return result instanceof String ? (String) result : JSONUtil.toJsonStr(result);
            }

            // 如果没有result字段，返回原始响应
            return response;

        } catch (Exception e) {
            log.warn("[JSON-RPC调用] 响应解析失败，返回原始响应: {}", e.getMessage());
            return response;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(JSON_RPC, this);
    }
}