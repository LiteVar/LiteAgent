package com.litevar.agent.core.module.tool.parser;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.litevar.agent.base.dto.ModbusJsonDTO;
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
import java.util.List;
import java.util.Set;

/**
 * modbus协议解析
 *
 * @author reid
 * @since 2024/8/5
 */

@Component
public class ToolOpenModbusParser implements ToolParser, InitializingBean {
    private ObjectMapper objectMapper;

    @Override
    public List<ToolFunction> parse(String rawStr) {
        ModbusJsonDTO dto;
        try {
            dto = getObjectMapper().readValue(rawStr, ModbusJsonDTO.class);
        } catch (JsonProcessingException e) {
            throw new ServiceException(1000, "解析失败!");
        }
        checkData(dto);

        List<ToolFunction> functionList = new ArrayList<>();
        dto.getFunctions().forEach(function -> {
            ToolFunction toolFunction = new ToolFunction();
            toolFunction.setProtocol(FunctionExecutor.MODBUS);
            toolFunction.setResource(function.getName());
            toolFunction.setDescription(function.getDescription());
            toolFunction.setRequestMethod(function.getMethod().name());
            if (function.getMethod() == ModbusJsonDTO.Method.write) {
                ToolFunction.ParameterInfo paramInfo = new ToolFunction.ParameterInfo();
                ModbusJsonDTO.Parameter parameter = function.getParameter();
                paramInfo.setParamName(parameter.getName());
                paramInfo.setDescription(parameter.getDescription());
                paramInfo.setRequired(true);
                String paramType = switch (parameter.getType()) {
                    case bool -> "boolean";
                    case string -> "string";
                    case int16, int32, uint16, uint32 -> "integer";
                };
                paramInfo.setType(paramType);
                toolFunction.getParameters().add(paramInfo);
            }

            JSONObject extra = new JSONObject()
                    .set("server", dto.getServer())
                    .set("path", function.getPath())
                    .set("return", function.getReturnValue())
                    .set("parameter", function.getParameter());
            toolFunction.setExtra(extra.toString());

            functionList.add(toolFunction);
        });

        return functionList;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerParser(ToolSchemaType.OPEN_MODBUS, this);
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        return objectMapper;
    }

    private void checkData(ModbusJsonDTO dto) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<ModbusJsonDTO>> errorMessages = factory.getValidator().validate(dto);
            if (!errorMessages.isEmpty()) {
                throw new ServiceException(1000, "解析失败:" + StrUtil.join(",", errorMessages));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(1000, "解析异常!");
        }

        dto.getFunctions().forEach(function -> {
            if (function.getMethod() == ModbusJsonDTO.Method.read && function.getReturnValue() == null) {
                throw new ServiceException(1000, "function name:" + function.getName() + " 'return' cannot be null");
            } else if (function.getMethod() == ModbusJsonDTO.Method.write && function.getParameter() == null) {
                throw new ServiceException(1000, "function name:" + function.getName() + " 'parameter' cannot be null");
            }
            ModbusJsonDTO.Storage storage = function.getPath().getStorage();
            if (function.getMethod() == ModbusJsonDTO.Method.write &&
                    (storage == ModbusJsonDTO.Storage.inputRegisters || storage == ModbusJsonDTO.Storage.discreteInput)) {
                throw new ServiceException(1000, "function name:" + function.getName() + " 'method' value 'write' illegal");
            }
        });
    }
}
