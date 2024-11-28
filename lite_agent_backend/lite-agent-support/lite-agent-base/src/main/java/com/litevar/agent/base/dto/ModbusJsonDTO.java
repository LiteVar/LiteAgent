package com.litevar.agent.base.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * modbus tool schema
 * <p>
 * 参照<a href="https://github.com/LiteVar/openmodbus_dart/blob/main/openmodbus-specification-en.md">OpenModbus规范</a>
 * </p>
 *
 * @author uncle
 * @since 2024/11/8 10:07
 */
@Data
public class ModbusJsonDTO {
    /**
     * 当前modbus设备的访问信息
     */
    @Valid
    @NotNull(message = "server不能为空")
    private Server server;

    /**
     * 当前工具调用支持的函数信息
     */
    @Valid
    @NotNull(message = "functions不能为空")
    private List<Function> functions;

    @Data
    public static class Server {
        /**
         * tcp,udp,rtu,ascii
         */
        @NotNull(message = "server.type不能为空")
        private CallType type;
        @NotNull(message = "server.config不能为空")
        private Config config;
    }

    @Data
    public static class Function {
        /**
         * 函数名字
         */
        @NotBlank(message = "函数名称[name]不能为空")
        @Size(min = 1, max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "函数的名称只包含a-z、A-Z、0-9、-、_符号")
        private String name;
        /**
         * 函数描述
         */
        private String description;
        /**
         * 函数读写类型(read,write)
         */
        @NotNull(message = "函数读写类型[method]不能为空")
        private Method method;

        @Valid
        @NotNull
        private Path path;
        /**
         * 函数的入参描述,当method=write,则为必填
         */
        @Valid
        private Parameter parameter;
        /**
         * 函数返回参数描述,当method=read,则为必填
         */
        @Valid
        @JsonAlias("return")
        private Parameter returnValue;
    }

    @Data
    public static class Config {
        /**
         * 当type=tcp,udp时,网络设备域名或IP
         */
        private String url;
        /**
         * 当type=tcp,udp时,网络设备的端口,例如502
         * 当type=rtu,ascii时,串口设备的识别名,例如COM3
         */
        @NotBlank(message = "config.port不能为空")
        private String port;
        /**
         * 当type=rtu,ascii时,串口设备的通信波特率,例如9600
         */
        private Integer baudRate;
    }

    @Data
    public static class Path {
        @NotNull(message = "path.slaveId 不能为空")
        private Integer slaveId;
        @NotNull(message = "path.storage 不能为空")
        private Storage storage;
        @NotNull(message = "path.address 不能为空")
        private Integer address;
        @NotNull(message = "path.count 不能为空")
        private Integer count;
    }

    public enum Storage {
        /**
         * 读写
         */
        coils, holdingRegisters,
        /**
         * 只读
         */
        inputRegisters, discreteInput
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
        /**
         * 参数类型(bool,int16,int32,uint16,uint32,string)
         */
        @NotNull(message = "参数类型[type]不能为空")
        private ParamType type;
        /**
         * 数值的倍数,默认为1,例如:返回uint16为328,multiplier为0.1,uom为℃,则可表示32.8℃
         */
        private Double multiplier = 1.0;
        /**
         * 参数的单位,例如:℃,kg
         */
        private String uom = "";
    }

    public enum Method {
        write, read
    }

    public enum CallType {
        tcp, udp,
        rtu, ascii
    }

    public enum ParamType {
        bool, int16, int32, uint16, uint32, string
    }
}
