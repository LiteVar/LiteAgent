package com.litevar.agent.base.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * open tool schema
 * <p>
 * 参照<a href="https://github.com/LiteVar/opentool_dart/blob/main/opentool-specification-en.md">OpenTool规范</a>
 * </p>
 *
 * @author uncle
 * @since 2025/4/9 11:20
 */
@Data
public class OpenToolJsonDTO {
    @Valid
    private List<Function> functions;


    @Data
    public static class Function {
        /**
         * 函数名称
         */
        @Pattern(regexp = "^[a-zA-Z0-9_-]{1,64}$", message = "函数的名称只包含a-z、A-Z、0-9、-、_符号,且长度不能超过64个字符")
        private String name;
        /**
         * 函数描述
         */
        private String description;
        /**
         * 函数入参
         */
        @Valid
        @NotNull
        private List<Parameter> parameters;
    }

    @Data
    public static class Parameter {
        /**
         * 参数名称
         */
        @NotBlank
        private String name;
        /**
         * 参数描述
         */
        private String description;

        @Valid
        @NotNull
        private Schema schema;
        /**
         * 参数是否必填
         */
        @NotNull
        private Boolean required;
    }

    @Data
    public static class Schema {
        /**
         * 类型
         */
        @NotNull
        private ParamType type;
        private String description;
        /**
         * 当type=object时,子参数
         */
        @Valid
        private Map<String, Schema> properties;
        /**
         * 当type=array时,子参数
         */
        @Valid
        private Schema items;

        @JsonAlias("enum")
        private List<Object> enumValue;
        /**
         * 当type=object时,子参数必填字段
         */
        private List<String> required;
    }

    @Getter
    @AllArgsConstructor
    public enum ParamType {
        BOOLEAN("boolean"),
        INTEGER("integer"),
        NUMBER("number"),
        STRING("string"),
        ARRAY("array"),
        OBJECT("object");
        @JsonValue
        private final String value;
    }
}
