package com.litevar.agent.core.module.tool.parser;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.dto.OpenToolJsonDTO;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * open tool 解析
 *
 * @author uncle
 * @since 2025/9/4 10:44
 */
@Component
public class OpenToolParser implements ToolParser, InitializingBean {

    @Autowired
    private OpenToolThirdParser openToolThirdParser;

    @Override
    public List<ToolFunction> parse(String rawStr) {
        //{"origin": "server/input", "apiKey": "xxxx", "serverUrl": "http://xxx.com", "schema": "xxx"}
        JSONObject entries = JSONUtil.parseObj(rawStr);
        String serverUrl = entries.getStr("serverUrl");
        if (StrUtil.isBlank(serverUrl)) {
            throw new ServiceException(1000, "serverUrl字段不能为空");
        }

        String extraStr;
        if (StrUtil.isNotBlank(entries.getStr("apiKey"))) {
            extraStr = "{\"apiKey\":\"" + entries.getStr("apiKey") + "\"}";
        } else {
            extraStr = "{}";
        }

        OpenToolJsonDTO dto = openToolThirdParser.checkData(entries.getStr("schema"));
        List<ToolFunction> list = openToolThirdParser.parse(dto, FunctionExecutor.OPEN_TOOL);
        list.forEach(function -> {
            function.setServerUrl(serverUrl);
            function.setExtra(extraStr);
        });

        return list;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerParser(ToolSchemaType.OPEN_TOOL, this);
    }
}
