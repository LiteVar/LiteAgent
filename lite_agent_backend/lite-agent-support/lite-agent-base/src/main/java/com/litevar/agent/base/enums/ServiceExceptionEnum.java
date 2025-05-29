package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务异常枚举
 *
 * @author uncle
 * @since 2024/7/3 16:05
 */
@Getter
@AllArgsConstructor
public enum ServiceExceptionEnum {
    WRONG_ACCOUNT(10000, "账号信息错误"),
    ERROR_JWT_TOKEN(10001, "token格式不正确"),
    WITHOUT_TOKEN(10002, "请携带token访问接口"),
    BAD_TOKEN(10003, "无效token"),
    NO_PERMISSION(10004, "无权限访问该接口"),
    BAN_ACCOUNT(10005, "当前账号无法使用"),
    WRONG_PASSWORD(10006, "账号密码错误"),
    EXPIRED_LOGIN(10007, "登录已过期,请重新登录"),
    WORKSPACE_ID_HEADER_NULL(10008, "http header workspaceId不能为空"),
    REFERER_NOT_NULL(10009, "http header referer不能为空"),
    INVALID_APIKEY(10010, "无效的apikey"),
    CAPTCHA_EXPIRED(10011, "验证码已过期"),
    RESET_PWD_EXPIRED(10012, "重置密码链接已过期"),

    ARGUMENT_NOT_VALID(20000, "参数错误"),
    INVITE_EXPIRE(20001, "邀请已过期"),
    ACCOUNT_EXIST(20002, "账号已被注册过了"),
    DUPLICATE_WORKSPACE_NAME(20003, "工作空间名字重复"),
    DUPLICATE_EMAIL(20004, "存在已经加入过工作空间的邮箱"),
    NAME_DUPLICATE(20005, "名字不能重复"),
    NOT_FOUND_RECORD(20006, "不存在该记录"),
    NO_PERMISSION_OPERATE(20007, "没权限修改该数据"),


    ORIGIN_PASSWORD_WRONG(30001, "旧密码错误"),
    INIT_SESSION_FIRST(30002, "请先初始化会话"),
    AGENT_NOT_EXIST_OR_NOT_SHARE(30003, "agent不存在"),
    MODEL_NOT_EXIST_OR_NOT_SHARE(30004, "模型不存在"),
    MAX_TOKEN_LARGER(30005, "MaxToken值不能超过预设的值"),
    REFLECT_AGENT_CANNOT_CHAT(30006, "反思agent不支持直接聊天"),
    REFLECT_AGENT_WITHOUT_SUB_AGENT(30007, "反思agent不能有子agent"),
    REFLECT_AGENT_OVERSIZE(30008, "反思agent个数不能超过5个"),
    FUNCTION_NOT_CHOOSE(30010, "方法序列中有方法未选中"),
    TOOL_NO_FUNCTION(30011, "工具schema中未解析到function"),

    OPERATE_FAILURE(40001, "操作失败"),

    ;

    private final Integer code;
    private final String message;
}
