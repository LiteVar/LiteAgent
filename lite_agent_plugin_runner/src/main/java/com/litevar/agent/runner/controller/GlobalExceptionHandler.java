package com.litevar.agent.runner.controller;

import com.litevar.agent.runner.model.ErrorResponse;
import com.litevar.agent.runner.service.RunnerErrorCode;
import com.litevar.agent.runner.service.RunnerException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RunnerException.class)
    public ResponseEntity<ErrorResponse> handleRunnerException(RunnerException ex) {
        RunnerErrorCode errorCode = ex.getErrorCode();
        ErrorResponse body = new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), ex.getDetail());
        return new ResponseEntity<>(body, errorCode.getStatus());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
        RunnerErrorCode errorCode = RunnerErrorCode.INVALID_PARAM;
        ErrorResponse body = new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), ex.getMessage());
        return new ResponseEntity<>(body, errorCode.getStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadBody(HttpMessageNotReadableException ex) {
        RunnerErrorCode errorCode = RunnerErrorCode.INVALID_PARAM;
        ErrorResponse body = new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), ex.getMessage());
        return new ResponseEntity<>(body, errorCode.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        RunnerErrorCode errorCode = RunnerErrorCode.INTERNAL_ERROR;
        ErrorResponse body = new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
