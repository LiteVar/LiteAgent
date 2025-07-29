package com.litevar.agent.core.module.tool.serial;

import com.serotonin.modbus4j.serial.SerialPortWrapper;
import jssc.SerialPort;
import jssc.SerialPortException;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author uncle
 * @since 2024/11/21 10:26
 */
@Slf4j
public class SerialPortWrapperImpl implements SerialPortWrapper {
    private final SerialPort port;
    private final int baudRate;

    public SerialPortWrapperImpl(String port, int baudRate) {
        this.port = new SerialPort(port);
        this.baudRate = baudRate;
    }

    @Override
    public void close() throws Exception {
        port.closePort();
        log.info("Serial port closed:{}", port.getPortName());
    }

    @Override
    public void open() throws Exception {
        try {
            port.openPort();
            port.setParams(getBaudRate(), getDataBits(), getStopBits(), getParity());

            log.info("Serial port opened:{}", port.getPortName());

        } catch (SerialPortException ex) {
            log.error("open serial port:{} error:{}", port.getPortName(), ex.getMessage());
        }
    }

    @Override
    public InputStream getInputStream() {
        return new SerialInputStream(port);
    }

    @Override
    public OutputStream getOutputStream() {
        return new SerialOutputStream(port);
    }

    @Override
    public int getBaudRate() {
        return this.baudRate;
    }

    @Override
    public int getDataBits() {
        return SerialPort.DATABITS_8;
    }

    @Override
    public int getStopBits() {
        return SerialPort.STOPBITS_1;
    }

    @Override
    public int getParity() {
        return SerialPort.PARITY_NONE;
    }
}
