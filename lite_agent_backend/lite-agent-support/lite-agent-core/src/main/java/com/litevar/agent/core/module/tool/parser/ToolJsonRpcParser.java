package com.litevar.agent.core.module.tool.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author reid
 * @since 2024/8/5
 */
@Component
public class ToolJsonRpcParser implements ToolParser, InitializingBean {

    @Override
    public List<ToolFunction> parse(String rawStr) {
        JSONObject obj;
        try {
            obj = JSONUtil.parseObj(rawStr);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(1000, "解析失败");
        }
        List<String> error = new ArrayList<>();
        JSONArray servers = obj.getJSONArray("servers");
        String serverUrl = "";
        if (servers != null) {
            String url = ((JSONObject) servers.get(0)).getStr("url");
            if (StrUtil.isEmpty(url)) {
                error.add("server url is null");
            }
            if (!StrUtil.startWith(url, "http")) {
                error.add("rpc server url is only support http now");
            }
            serverUrl = url;
        } else {
            error.add("server url is null");
        }

        List<ToolFunction> list = new ArrayList<>();

        // 获取全局参数传递方式配置，默认为命名参数
        String globalParamStyle = "named";
        if (obj.containsKey("paramStyle")) {
            globalParamStyle = obj.getStr("paramStyle", "named");
        }

        JSONArray functions = obj.getJSONArray("methods");
        if (functions == null || functions.isEmpty()) {
            error.add("methods is null");
        } else {

            for (Object m : functions) {
                JSONObject function = (JSONObject) m;
                String functionName = function.getStr("name");
                if (StrUtil.isEmpty(functionName)) {
                    error.add("method name has null");
                    break;
                }
                ToolFunction dto = new ToolFunction();
                dto.setServerUrl(serverUrl);
                dto.setContentType("application/json");
                dto.setRequestMethod("post");
                dto.setResource(functionName);
                dto.setProtocol(FunctionExecutor.JSON_RPC);
                dto.setDescription(function.getStr("description"));

                // 设置参数传递方式（优先使用方法级别的配置，否则使用全局配置）
                String paramStyle = function.getStr("paramStyle", globalParamStyle);
                JSONObject extraConfig = new JSONObject();
                extraConfig.set("paramStyle", paramStyle);
                dto.setExtra(JSONUtil.toJsonStr(extraConfig));

                JSONArray params = function.getJSONArray("params");
                if (params != null && !params.isEmpty()) {
                    for (Object p : params) {
                        JSONObject param = (JSONObject) p;
                        String paramName = param.getStr("name");
                        if (StrUtil.isEmpty(paramName)) {
                            error.add("param name has null");
                            break;
                        }
                        ToolFunction.ParameterInfo paramInfo = travelParam(param, error, 10);

                        if (paramInfo != null) {
                            paramInfo.setParamName(paramName);
                            paramInfo.setIn(FunctionExecutor.BODY);
                            dto.getParameters().add(paramInfo);
                        }
                    }
                }
                list.add(dto);
            }
        }
        if (!error.isEmpty()) {
            throw new ServiceException(1000, "解析异常:" + StrUtil.join(",", error));
        }
        return list;
    }

    public ToolFunction.ParameterInfo travelParam(JSONObject param, List<String> error, int deep) {
        ToolFunction.ParameterInfo paramInfo = new ToolFunction.ParameterInfo();
        while (deep-- > 0) {
            paramInfo.setDescription(param.getStr("description"));
            paramInfo.setRequired(param.getBool("required", false));

            String paramType = param.getStr("type");
            if (StrUtil.isEmpty(paramType)) {
                JSONObject schema = param.getJSONObject("schema");
                if (schema != null) {
                    paramType = schema.getStr("type");
                }
            }
            if (StrUtil.isEmpty(paramType)) {
                error.add("param type has null");
                return null;
            }
            paramInfo.setType(paramType);

            if (ObjectUtil.isNotEmpty(param.getJSONArray("enum"))) {
                param.getJSONArray("enum").forEach(v -> paramInfo.getEnums().add(v));
            }

            int currentDeep = deep;
            if (StrUtil.equals(paramType, "object")) {
                JSONObject subParam = param.getJSONObject("schema").getJSONObject("properties");
                if (subParam != null) {
                    subParam.forEach((paramName, v) -> {
                        JSONObject info = (JSONObject) v;
                        ToolFunction.ParameterInfo subParamInfo = travelParam(info, error, currentDeep);
                        if (subParamInfo != null) {
                            subParamInfo.setParamName(paramName);
                            paramInfo.getProperties().add(subParamInfo);
                        }
                    });
                }
            } else if (StrUtil.equals(paramType, "array")) {
                JSONObject items = param.getJSONObject("schema").getJSONObject("items");
                if (items != null) {
                    ToolFunction.ParameterInfo subParamInfo = travelParam(items, error, currentDeep);
                    if (subParamInfo != null) {
                        subParamInfo.setParamName("");
                        paramInfo.getProperties().add(subParamInfo);
                    }
                }
            }
            break;
        }
        return paramInfo;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerParser(ToolSchemaType.JSON_RPC, this);
    }
}
