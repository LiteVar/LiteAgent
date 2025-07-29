package com.litevar.agent.base.exception;

import com.litevar.agent.base.enums.ServiceExceptionEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author uncle
 * @since 2024/7/3 16:07
 */
@Getter
@AllArgsConstructor
public class ServiceException extends RuntimeException {
    private final Integer code;
    private final String message;

    public ServiceException(ServiceExceptionEnum serviceExceptionEnum) {
        super(serviceExceptionEnum.getMessage());
        this.code = serviceExceptionEnum.getCode();
        this.message = serviceExceptionEnum.getMessage();
    }

    public ServiceException(String message) {
        super(message);
        this.code = 10000;
        this.message = message;
    }
}
