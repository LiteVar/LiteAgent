package com.litevar.agent.core.module.tool.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * open tool 解析
 *
 * @author uncle
 * @since 2025/4/9 11:04
 */
@Component
public class OpenToolParser implements ToolParser, InitializingBean {
    private ObjectMapper objectMapper;

    @Override
    public List<ToolFunction> parse(String rawStr) {
        OpenToolJsonDTO dto;
        try {
            dto = getObjectMapper().readValue(rawStr, OpenToolJsonDTO.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new ServiceException(1000, "解析失败!");
        }
        checkData(dto);

        List<ToolFunction> functionList = new ArrayList<>();
        dto.getFunctions().forEach(function -> {
            ToolFunction toolFunction = new ToolFunction();
            toolFunction.setProtocol(FunctionExecutor.EXTERNAL);
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
        ToolHandleFactory.registerParser(ToolSchemaType.OPEN_TOOL, this);
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
    }

    private void checkData(OpenToolJsonDTO dto) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<OpenToolJsonDTO>> errorMessages = factory.getValidator().validate(dto);
            if (!errorMessages.isEmpty()) {
                throw new ServiceException(1000, "解析失败:" + StrUtil.join(",", errorMessages));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(1000, "解析异常!");
        }
    }
}
