package com.litevar.agent.core.module.tool.executor;

import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.opentool.client.OpenToolClient;
import com.litevar.opentool.model.FunctionCall;
import com.litevar.opentool.model.ToolReturn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

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
        FunctionCall functionCall = new FunctionCall();
        functionCall.setId(callId);
        functionCall.setName(info.getResource());
        functionCall.setArguments(data);

        ToolReturn result = client.call(functionCall);
        return JSONUtil.toJsonStr(result.getResult());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(OPEN_TOOL, this);
    }
}
