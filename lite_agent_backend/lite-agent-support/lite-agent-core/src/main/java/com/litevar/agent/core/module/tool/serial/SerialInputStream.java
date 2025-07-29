package com.litevar.agent.core.module.tool.serial;

import jssc.SerialPort;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author uncle
 * @since 2024/11/20 17:21
 */
public class SerialInputStream extends InputStream {
    private final SerialPort port;
    @Setter
    private int timeout = 0;

    public SerialInputStream(SerialPort port) {
        this.port = port;
    }

    @Override
    public int read() throws IOException {
        return read(timeout);
    }

    public int read(int timeout) throws IOException {
        byte[] buffer;
        try {
            if (timeout > 0) {
                buffer = port.readBytes(1, timeout);
            } else {
                buffer = port.readBytes(1);
            }
            return buffer[0];
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        if (buffer.length < off + len) {
            len = buffer.length - off;
        }

        int available = this.available();
        if (available > len) {
            available = len;
        }
        try {
            byte[] readBuf = port.readBytes(available);
            System.arraycopy(readBuf, 0, buffer, off, readBuf.length);
            return readBuf.length;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public int available() throws IOException {
        try {
            int ret;
            ret = port.getInputBufferBytesCount();
            if (ret >= 0) {
                return ret;
            }
            throw new IOException("error checking available bytes from the serial port.");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
