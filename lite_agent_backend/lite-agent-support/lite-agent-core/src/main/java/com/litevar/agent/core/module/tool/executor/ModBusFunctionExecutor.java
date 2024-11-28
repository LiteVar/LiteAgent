package com.litevar.agent.core.module.tool.executor;

import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * function mod bus 协议调用
 *
 * @author uncle
 * @since 2024/10/18 15:17
 */
@Component
public class ModBusFunctionExecutor implements FunctionExecutor, InitializingBean {

    @Override
    public String invoke(ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        return "";
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(FunctionExecutor.MODBUS, this);
    }
}
