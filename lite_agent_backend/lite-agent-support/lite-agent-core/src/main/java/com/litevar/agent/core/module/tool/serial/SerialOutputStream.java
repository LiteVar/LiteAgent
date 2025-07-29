package com.litevar.agent.core.module.tool.serial;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author uncle
 * @since 2024/11/20 17:16
 */
public class SerialOutputStream extends OutputStream {
    private final SerialPort port;

    public SerialOutputStream(SerialPort port) {
        this.port = port;
    }

    @Override
    public void write(int b) throws IOException {
        try {
            port.writeInt(b);
        } catch (SerialPortException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] buffer = new byte[len];
        System.arraycopy(b, off, buffer, 0, len);
        try {
            port.writeBytes(buffer);
        } catch (SerialPortException e) {
            throw new IOException(e);
        }
    }
}
