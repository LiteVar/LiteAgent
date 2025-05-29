package com.litevar.agent.core.module.tool.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.executor.FunctionExecutor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * openapi 解析
 *
 * @author reid
 * @since 2024/8/5
 */
@Component
public class ToolOpenApiParser implements ToolParser, InitializingBean {

    @Override
    public List<ToolFunction> parse(String rawStr) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(true);

        SwaggerParseResult parseResult;
        try {
            parseResult = new OpenAPIV3Parser().readContents(rawStr, null, parseOptions);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(1000, "解析失败");
        }
        if (!parseResult.getMessages().isEmpty()) {
            throw new ServiceException(1000, "解析异常: " + StrUtil.join(",", parseResult.getMessages()));
        }

        OpenAPI openAPI = parseResult.getOpenAPI();
        List<Server> servers = openAPI.getServers();
        if (servers.isEmpty() || servers.get(0).getUrl().equals("/")) {
            throw new ServiceException(1000, "解析异常: server url 不能为空");
        }

        String url = servers.get(0).getUrl();

        List<ToolFunction> res = new ArrayList<>();
        openAPI.getPaths().forEach((path, info) -> {
            Map<String, Operation> operations = new HashMap<>(4);
            operations.put("get", info.getGet());
            operations.put("post", info.getPost());
            operations.put("put", info.getPut());
            operations.put("delete", info.getDelete());

            operations.forEach((method, operation) -> {
                if (operation != null) {
                    ToolFunction dto = resolveParam(operation);
                    dto.setServerUrl(url);
                    dto.setResource(path);
                    dto.setRequestMethod(method);
                    res.add(dto);
                }
            });
            operations.clear();
        });

        return res;
    }

    private ToolFunction resolveParam(Operation operation) {
        ToolFunction dto = new ToolFunction();
        dto.setDescription(operation.getSummary());
        dto.setProtocol(FunctionExecutor.HTTP);

        //body字段信息
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody != null) {
            Map.Entry<String, MediaType> entry = requestBody.getContent().entrySet().iterator().next();
            String contentType = entry.getKey();
            Schema schema = entry.getValue().getSchema();

            dto.setContentType(contentType);
            List<String> required = schema.getRequired() == null ? Collections.emptyList() : schema.getRequired();
            String type = schema.getType();
            if (StrUtil.equals(type, "object")) {
                Map<String, Schema> properties = schema.getProperties();
                if (properties != null) {
                    properties.forEach((fieldName, subSchema) -> {
                        ToolFunction.ParameterInfo param = travelParam(subSchema, 10);
                        param.setParamName(fieldName);
                        param.setIn(FunctionExecutor.BODY);
                        param.setRequired(required.contains(fieldName));
                        dto.getParameters().add(param);
                    });
                }

            } else if (StrUtil.equals(type, "array")) {
                //暂不支持最外层是数组
//                Schema items = schema.getItems();
//                ToolFunction.ParameterInfo param = travelParam(items, 10);
//                param.setParamName(items.getName());
//                param.setRequired(required.contains(items.getName()));
            }
        }

        //query,header,path字段信息
        List<Parameter> parameters = operation.getParameters();
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                ToolFunction.ParameterInfo param = new ToolFunction.ParameterInfo();
                param.setParamName(parameter.getName());
                param.setDescription(parameter.getDescription());
                param.setRequired(parameter.getRequired());
                param.setType(parameter.getSchema().getType());
                param.setIn(parameter.getIn());
                List enums = parameter.getSchema().getEnum();
                if (ObjectUtil.isNotEmpty(enums)) {
                    enums.forEach(v -> param.getEnums().add(v.toString()));
                }
                dto.getParameters().add(param);
            }
        }

        return dto;
    }

    private ToolFunction.ParameterInfo travelParam(Schema schema, int deep) {
        ToolFunction.ParameterInfo param = new ToolFunction.ParameterInfo();
        while (deep-- > 0) {
            String type = schema.getType();
            param.setType(type);
            param.setDescription(schema.getDescription());
            if (ObjectUtil.isNotEmpty(schema.getEnum())) {
                schema.getEnum().forEach(v -> param.getEnums().add(v.toString()));
            }
            if (ObjectUtil.isNotEmpty(schema.getDefault())) {
                param.setDefaultValue(schema.getDefault());
            }
            List<String> subRequired = schema.getRequired() == null ? Collections.emptyList() : schema.getRequired();
            int currentDeep = deep;
            if (StrUtil.equals(type, "object")) {
                Map<String, Schema> properties = schema.getProperties();
                if (properties != null) {
                    properties.forEach((fieldName, subSchema) -> {
                        ToolFunction.ParameterInfo subParam = travelParam(subSchema, currentDeep);
                        subParam.setParamName(fieldName);
                        subParam.setRequired(subRequired.contains(fieldName));
                        param.getProperties().add(subParam);
                    });
                }
            } else if (StrUtil.equals(type, "array")) {
                Schema items = schema.getItems();
                ToolFunction.ParameterInfo subParam = travelParam(items, currentDeep);
                subParam.setParamName(items.getName());
                subParam.setRequired(subRequired.contains(subParam.getParamName()));
                param.getProperties().add(subParam);
            }
            break;
        }
        return param;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerParser(ToolSchemaType.OPEN_API3, this);
    }
}
