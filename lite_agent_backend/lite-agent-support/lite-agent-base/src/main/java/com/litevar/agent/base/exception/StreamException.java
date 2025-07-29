package com.litevar.agent.base.exception;

import lombok.Getter;

/**
 * @author uncle
 * @since 2025/5/6 12:23
 */
@Getter
public class StreamException extends RuntimeException {
    private final Integer code;
    private final String message;

    public StreamException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
