package com.litevar.agent.base.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具解析后的接口信息
 *
 * @author uncle
 * @since 2024/10/16 16:39
 */
@Data
@Document("tool_function")
public class ToolFunction {
    @Id
    private String id;

    @Indexed
    private String toolId;

    /**
     * 调用协议(http,modbus)
     */
    private String protocol;

    /**
     * 服务地址
     */
    private String serverUrl;
    /**
     * 函数路径
     */
    private String resource;
    /**
     * 请求方法:get,post,put,delete
     * modbus: read,write
     */
    private String requestMethod;
    /**
     * 传参方式(multipart/form-data,application/json)
     */
    private String contentType;
    /**
     * 函数描述
     */
    private String description;
    /**
     * 参数列表
     */
    private List<ParameterInfo> parameters = new ArrayList<>();
    /**
     * modbus协议server,path,return值的json字符串
     */
    private String extra;

    @Data
    public static class ParameterInfo {
        /**
         * 参数位置(body,query,header,path)
         */
        private String in;
        /**
         * 参数名字
         */
        private String paramName;
        /**
         * 参数描述
         */
        private String description;
        /**
         * 参数类型(int,String,double....)
         */
        private String type;
        /**
         * 是否必传
         */
        private boolean required = false;
        /**
         * 枚举值
         */
        private List<String> enums = new ArrayList<>();
        /**
         * 默认值
         */
        private Object defaultValue;
        /**
         * object,array类型,子字段
         */
        private List<ParameterInfo> properties = new ArrayList<>();
    }
}
