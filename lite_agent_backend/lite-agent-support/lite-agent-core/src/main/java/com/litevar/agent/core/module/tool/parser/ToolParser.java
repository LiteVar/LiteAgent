package com.litevar.agent.core.module.tool.parser;

import com.litevar.agent.base.entity.ToolFunction;

import java.util.List;

/**
 * @author reid
 * @since 2024/8/5
 */
public interface ToolParser {

    List<ToolFunction> parse(String rawStr);

}
