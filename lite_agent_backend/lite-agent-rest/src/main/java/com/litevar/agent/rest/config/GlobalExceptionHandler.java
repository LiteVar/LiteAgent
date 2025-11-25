package com.litevar.agent.rest.config;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.base.response.ResponseData;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 全局异常处理器
 *
 * @author uncle
 * @since 2024/7/4 16:49
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ServiceException.class)
    public ResponseData<String> businessException(ServiceException ex) {
        ex.printStackTrace();
        return renderJson(ex.getCode(), ex.getMessage(), ex);
    }

    @ExceptionHandler({StreamException.class, AsyncRequestTimeoutException.class})
    public void streamException(Exception ex, HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setCharacterEncoding(CharsetUtil.UTF_8);
        ex.printStackTrace();
        try (PrintWriter writer = response.getWriter()) {
            String str;
            if (ex instanceof StreamException e) {
                str = JSONUtil.toJsonStr(renderJson(e.getCode(), e.getMessage(), e));
            } else {
                str = JSONUtil.toJsonStr(renderJson(500, ex.getMessage(), ex));
            }
            writer.write(str);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseData<String> notFoundException(NoSuchElementException ex) {
        ex.printStackTrace();
        return renderJson(HttpStatus.NOT_FOUND.value(), "不存在相关记录", ex);
    }

    /**
     * 参数格式传递异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseData<String> httpMessageNotReadable(HttpMessageNotReadableException ex) {
        ex.printStackTrace();
        return renderJson(400, buildHttpMessageNotReadableMessage(ex));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseData<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> {
                    String defaultMessage = error.getDefaultMessage();
                    String rejectedValue = formatValue(error.getRejectedValue());
                    if (defaultMessage == null || defaultMessage.isBlank()) {
                        defaultMessage = "不符合要求";
                    }
                    return "参数`" + error.getField() + "`" + defaultMessage + "，实际值`" + rejectedValue + "`";
                })
                .orElse("参数校验失败");
        e.printStackTrace();
        return renderJson(400, message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseData<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        e.printStackTrace();
        String parameterName = e.getName();
        String requiredType = e.getRequiredType() == null ? "未知类型" : e.getRequiredType().getSimpleName();
        String value = formatValue(e.getValue());
        return renderJson(400, "参数`" + parameterName + "`类型错误，值`" + value + "`无法转换为`" + requiredType + "`");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseData<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String parameterName = e.getParameterName();
        String parameterType = e.getParameterType();
        if (parameterType == null || parameterType.isBlank()) {
            parameterType = "未指定";
        }
        return renderJson(400, "缺少必需参数`" + parameterName + "`，期望类型`" + parameterType + "`");
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseData<String> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream().findFirst()
                .map(violation -> buildConstraintViolationMessage(violation))
                .orElse("参数校验失败");
        return renderJson(400, message);
    }

    /**
     * 未知的运行时异常
     */
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseData<String> serverError(Throwable e) {
        e.printStackTrace();
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        return renderJson(500, e.getMessage(), e);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseData<String> exception(Exception ex) {
        ex.printStackTrace();
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        return renderJson(500, ex.getMessage(), ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseData<String> handleNoResourceFoundException(NoResourceFoundException ex) {
        return renderJson(404, ex.getMessage());
    }

    private ResponseData<String> renderJson(Integer code, String message) {
        return renderJson(code, message, null);
    }


    /**
     * 渲染异常返回json
     * 根据异常枚举和Throwable异常响应，异常信息响应堆栈第一行
     */
    private ResponseData<String> renderJson(Integer code, String message, Throwable e) {
        if (ObjectUtil.isNotNull(e)) {

            //获取所有堆栈信息
            StackTraceElement[] stackTraceElements = e.getStackTrace();

            //默认的异常类全路径为第一条异常堆栈信息的
            String exceptionClassTotalName = stackTraceElements[0].toString();

            //遍历所有堆栈信息，找到com.litevar.agent开头的第一条异常信息
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                if (stackTraceElement.toString().contains("com.litevar.agent")) {
                    exceptionClassTotalName = stackTraceElement.toString();
                    break;
                }
            }
            return ResponseData.error(code, message, exceptionClassTotalName);
        } else {
            return ResponseData.error(code, message);
        }
    }

    private String buildConstraintViolationMessage(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        String parameterName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        if (parameterName.isBlank()) {
            parameterName = "参数";
        }
        String violationMessage = violation.getMessage();
        if (violationMessage == null || violationMessage.isBlank()) {
            violationMessage = "不满足约束";
        }
        String invalidValueStr = formatValue(violation.getInvalidValue());
        return "参数`" + parameterName + "`" + violationMessage + "，实际值`" + invalidValueStr + "`";
    }

    private String buildHttpMessageNotReadableMessage(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            String path = buildJsonPath(invalidFormatException.getPath());
            String targetType = invalidFormatException.getTargetType() == null ? "未知类型" : invalidFormatException.getTargetType().getSimpleName();
            String value = formatValue(invalidFormatException.getValue());
            return "请求体字段`" + path + "`类型错误，值`" + value + "`无法转换为`" + targetType + "`";
        }
        if (cause instanceof MismatchedInputException mismatchedInputException) {
            String path = buildJsonPath(mismatchedInputException.getPath());
            String targetType = mismatchedInputException.getTargetType() == null ? "未知类型" : mismatchedInputException.getTargetType().getSimpleName();
            if (path.isBlank() || "未知字段".equals(path)) {
                return "请求体结构不正确，无法解析为`" + targetType + "`";
            }
            return "请求体字段`" + path + "`结构不正确，无法解析为`" + targetType + "`";
        }
        if (cause instanceof JsonMappingException jsonMappingException) {
            String path = buildJsonPath(jsonMappingException.getPath());
            String originalMessage = jsonMappingException.getOriginalMessage();
            if (originalMessage == null || originalMessage.isBlank()) {
                originalMessage = "解析失败";
            }
            if (path.isBlank() || "未知字段".equals(path)) {
                return "请求体解析失败：" + originalMessage;
            }
            return "请求体字段`" + path + "`解析失败：" + originalMessage;
        }
        if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return "请求体解析失败：" + cause.getMessage();
        }
        return "请求体格式不正确";
    }

    private String buildJsonPath(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "未知字段";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonMappingException.Reference reference : path) {
            String fieldName = reference.getFieldName();
            if (fieldName != null) {
                if (builder.length() > 0) {
                    builder.append('.');
                }
                builder.append(fieldName);
            }
            if (reference.getIndex() >= 0) {
                builder.append('[').append(reference.getIndex()).append(']');
            }
        }
        return builder.length() == 0 ? "未知字段" : builder.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        String valueStr = String.valueOf(value);
        if (valueStr.length() > 100) {
            return valueStr.substring(0, 100) + "...";
        }
        return valueStr;
    }
}
