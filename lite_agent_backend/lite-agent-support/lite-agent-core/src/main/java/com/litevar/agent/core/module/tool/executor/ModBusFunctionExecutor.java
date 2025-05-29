package com.litevar.agent.core.module.tool.executor;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.dto.ModbusJsonDTO;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.serial.SerialPortWrapperImpl;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * function mod bus 协议调用
 *
 * @author uncle
 * @since 2024/10/18 15:17
 */
@Component
public class ModBusFunctionExecutor implements FunctionExecutor, InitializingBean {

    @Override
    public String invoke(String callId, ToolFunction info, Map<String, Object> data, Map<String, String> defineHeader) {
        JSONObject obj = JSONUtil.parseObj(info.getExtra());
        ModbusJsonDTO.Path path = obj.getJSONObject("path").toBean(ModbusJsonDTO.Path.class);
        ModbusJsonDTO.Server server = obj.getJSONObject("server").toBean(ModbusJsonDTO.Server.class);

        ModbusMaster master = getModbusMaster(server);
        if (StrUtil.equals(info.getRequestMethod(), "read")) {
            ModbusJsonDTO.Parameter returnValue = obj.getJSONObject("return").toBean(ModbusJsonDTO.Parameter.class);
            try {
                switch (path.getStorage()) {
                    case coils -> {
                        BaseLocator<Boolean> loc = BaseLocator.coilStatus(path.getSlaveId(), path.getAddress());
                        Boolean value = master.getValue(loc);
                    }

                    case holdingRegisters -> {
                        BaseLocator<Number> loc = BaseLocator.holdingRegister(path.getSlaveId(), path.getAddress(), DataType.FOUR_BYTE_INT_SIGNED);
                        Number value = master.getValue(loc);
                    }
                    case inputRegisters -> {
                        BaseLocator<Number> loc = BaseLocator.inputRegister(path.getSlaveId(), path.getAddress(), DataType.FOUR_BYTE_INT_SIGNED);
                        Number value = master.getValue(loc);
                    }
                    case discreteInput -> {
                        BaseLocator<Boolean> loc = BaseLocator.inputStatus(path.getSlaveId(), path.getAddress());
                        Boolean value = master.getValue(loc);
                    }
                }

            } catch (ModbusTransportException | ErrorResponseException ex) {
                ex.printStackTrace();
            }
        } else {
            //write
            ModbusJsonDTO.Parameter parameter = obj.getJSONObject("parameter").toBean(ModbusJsonDTO.Parameter.class);
        }

        master.destroy();
        return "";
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ToolHandleFactory.registerFunctionExecutor(FunctionExecutor.MODBUS, this);
    }

    private ModbusMaster getModbusMaster(ModbusJsonDTO.Server server) {
        ModbusFactory modbusFactory = new ModbusFactory();
        ModbusMaster modbusMaster = switch (server.getType()) {
            case rtu -> {
                SerialPortWrapperImpl wrapper = new SerialPortWrapperImpl(server.getConfig().getPort(), server.getConfig().getBaudRate());
                yield modbusFactory.createRtuMaster(wrapper);
            }
            case ascii -> {
                SerialPortWrapperImpl wrapper = new SerialPortWrapperImpl(server.getConfig().getPort(), server.getConfig().getBaudRate());
                yield modbusFactory.createAsciiMaster(wrapper);
            }
            case tcp -> {
                IpParameters params = new IpParameters();
                params.setHost(server.getConfig().getUrl());
                params.setPort(NumberUtil.parseInt(server.getConfig().getPort()));
                //modbus tcp/ip时为false,modbus rtu over tcp/ip时为true
                params.setEncapsulated(false);

                yield modbusFactory.createTcpMaster(params, false);
            }
            case udp -> {
                IpParameters params = new IpParameters();
                params.setHost(server.getConfig().getUrl());
                params.setPort(NumberUtil.parseInt(server.getConfig().getPort()));
                //modbus tcp/ip时为false,modbus rtu over tcp/ip时为true
                params.setEncapsulated(false);

                yield modbusFactory.createUdpMaster(params);
            }
        };

        modbusMaster.setTimeout(2000);
        modbusMaster.setRetries(4);
        try {
            modbusMaster.init();
        } catch (ModbusInitException e) {
            throw new RuntimeException(e);
        }
        return modbusMaster;
    }

}
