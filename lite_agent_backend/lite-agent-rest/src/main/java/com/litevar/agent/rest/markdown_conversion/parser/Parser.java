package com.litevar.agent.rest.markdown_conversion.parser;

import com.litevar.agent.rest.markdown_conversion.core.ConversionOptions;
import com.litevar.agent.rest.markdown_conversion.core.ConversionResult;
import com.litevar.agent.rest.markdown_conversion.core.model.Blocks;

import java.nio.file.Path;

public interface Parser {
    Blocks.Document parse(Path file, ConversionOptions options, ConversionResult result) throws Exception;
}

