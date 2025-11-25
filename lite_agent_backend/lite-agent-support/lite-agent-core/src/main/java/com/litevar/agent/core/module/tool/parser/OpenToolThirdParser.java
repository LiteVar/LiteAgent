package com.litevar.agent.core.module.tool.parser;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.dto.OpenToolJsonDTO;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    @Override
    public List<ToolFunction> parse(String rawStr) {
        OpenToolJsonDTO dto = checkData(rawStr);
        return parse(dto, FunctionExecutor.EXTERNAL);
    }

    public List<ToolFunction> parse(OpenToolJsonDTO dto, String callProtocol) {
        List<ToolFunction> functionList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(dto.getFunctions())) {
            dto.getFunctions().forEach(function -> {
                ToolFunction toolFunction = new ToolFunction();
                toolFunction.setProtocol(callProtocol);
                toolFunction.setResource(function.getName());
                toolFunction.setDescription(function.getDescription());
                if (ObjectUtil.isNotEmpty(function.getParameters())) {
                    function.getParameters().forEach(param -> {
                        ToolFunction.ParameterInfo paramInfo = new ToolFunction.ParameterInfo();
                        paramInfo.setParamName(param.getName());
                        paramInfo.setDescription(param.getDescription());
                        paramInfo.setRequired(param.getRequired());
                        resolveParam(paramInfo, param.getSchema());
                        toolFunction.getParameters().add(paramInfo);
                    });
                }
                functionList.add(toolFunction);
            });
        }
        return functionList;
    }

    private void resolveParam(ToolFunction.ParameterInfo paramInfo, OpenToolJsonDTO.Schema schema) {
        OpenToolJsonDTO.ParamType type = schema.getType();
        paramInfo.setType(type.getValue());
        paramInfo.setEnums(schema.getEnumValue());
        if (type == OpenToolJsonDTO.ParamType.OBJECT) {
            List<String> subRequired = ObjectUtil.isNotEmpty(schema.getRequired()) ? schema.getRequired() : Collections.emptyList();
            schema.getProperties().forEach((paramName, subSchema) -> {
                ToolFunction.ParameterInfo subParam = new ToolFunction.ParameterInfo();
                subParam.setParamName(paramName);
                subParam.setDescription(subSchema.getDescription());
                subParam.setRequired(subRequired.contains(paramName));
                resolveParam(subParam, subSchema);
                paramInfo.getProperties().add(subParam);
            });
        } else if (type == OpenToolJsonDTO.ParamType.ARRAY) {
            OpenToolJsonDTO.Schema subSchema = schema.getItems();
            ToolFunction.ParameterInfo subParam = new ToolFunction.ParameterInfo();
            subParam.setDescription(subSchema.getDescription());
            subParam.setEnums(subSchema.getEnumValue());
            resolveParam(subParam, subSchema);
            paramInfo.getProperties().add(subParam);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerParser(ToolSchemaType.OPEN_TOOL_Third, this);
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
    }

    public OpenToolJsonDTO checkData(String rawStr) {
        OpenToolJsonDTO dto;
        try {
            dto = getObjectMapper().readValue(rawStr, OpenToolJsonDTO.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new ServiceException(1000, "解析失败!");
        }
        Set<ConstraintViolation<OpenToolJsonDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String violationMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                    .orElse("验证失败");
            log.error("工具解析失败:{}", violationMessage);
            throw new ServiceException(1000, "解析异常");
        }
        return dto;
    }
}
