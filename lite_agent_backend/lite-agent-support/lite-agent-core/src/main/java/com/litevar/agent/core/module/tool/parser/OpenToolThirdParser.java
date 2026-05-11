package com.litevar.agent.core.module.tool.parser;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import com.litevar.opentool.model.OpenTool;
import com.litevar.opentool.model.Schema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * open tool 解析
 * 第三方系统回调结果
 *
 * @author uncle
 * @since 2025/4/9 11:04
 */
@Slf4j
@Component
public class OpenToolThirdParser implements ToolParser, InitializingBean {
    @Autowired
    private Validator validator;

    @Override
    public List<ToolFunction> parse(String rawStr) {
        OpenTool openTool = checkData(rawStr);
        return parse(openTool, FunctionExecutor.EXTERNAL);
    }

    public List<ToolFunction> parse(OpenTool openTool, String callProtocol) {
        List<ToolFunction> functionList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(openTool.getFunctions())) {
            openTool.getFunctions().forEach(function -> {
                ToolFunction toolFunction = new ToolFunction();
                toolFunction.setProtocol(callProtocol);
                toolFunction.setResource(function.getName());
                toolFunction.setDescription(function.getDescription());
                if (function.isStreamFunction()) {
                    toolFunction.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
                }
                if (ObjectUtil.isNotEmpty(function.getParameters())) {
                    function.getParameters().forEach(param -> {
                        ToolFunction.ParameterInfo paramInfo = new ToolFunction.ParameterInfo();
                        paramInfo.setParamName(param.getName());
                        paramInfo.setDescription(param.getDescription());
                        paramInfo.setRequired(param.isRequired());
                        resolveParam(paramInfo, param.getSchema());
                        toolFunction.getParameters().add(paramInfo);
                    });
                }
                functionList.add(toolFunction);
            });
        }
        return functionList;
    }

    private void resolveParam(ToolFunction.ParameterInfo paramInfo, Schema schema) {
        Schema.ParamType type = schema.getType();
        paramInfo.setType(type.getValue());
        if (ObjectUtil.isNotEmpty(schema.getEnumValues())) {
            schema.getEnumValues().forEach(v -> paramInfo.getEnums().add(v));
        }
        if (type == Schema.ParamType.OBJECT) {
            List<String> subRequired = ObjectUtil.isNotEmpty(schema.getRequired()) ? schema.getRequired() : Collections.emptyList();
            schema.getProperties().forEach((paramName, subSchema) -> {
                ToolFunction.ParameterInfo subParam = new ToolFunction.ParameterInfo();
                subParam.setParamName(paramName);
                subParam.setDescription(subSchema.getDescription());
                subParam.setRequired(subRequired.contains(paramName));
                resolveParam(subParam, subSchema);
                paramInfo.getProperties().add(subParam);
            });
        } else if (type == Schema.ParamType.ARRAY) {
            Schema subSchema = schema.getItems();
            ToolFunction.ParameterInfo subParam = new ToolFunction.ParameterInfo();
            subParam.setDescription(subSchema.getDescription());
            if (ObjectUtil.isNotEmpty(subSchema.getEnumValues())) {
                schema.getEnumValues().forEach(v -> subParam.getEnums().add(v));
            }
            resolveParam(subParam, subSchema);
            paramInfo.getProperties().add(subParam);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerParser(ToolSchemaType.OPEN_TOOL_Third, this);
    }

    public OpenTool checkData(String rawStr) {
        OpenTool openTool;
        try {
            openTool = OpenTool.fromJsonString(rawStr);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Set<ConstraintViolation<OpenTool>> validate = validator.validate(openTool);
        if (!validate.isEmpty()) {
            String violationMessage = validate.stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                    .orElse("验证失败");
            log.error("工具解析失败:{}", violationMessage);
            throw new ServiceException(1000, "解析异常");
        }
        return openTool;
    }
}
