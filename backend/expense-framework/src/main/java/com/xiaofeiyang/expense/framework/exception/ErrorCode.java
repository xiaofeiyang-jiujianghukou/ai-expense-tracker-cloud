package com.xiaofeiyang.expense.framework.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 400 - Bad Request
    PARAM_INVALID(40001, "参数校验失败"),
    EMAIL_ALREADY_EXISTS(40002, "邮箱已被注册"),

    // 401 - Unauthorized
    UNAUTHORIZED(40101, "未登录或 Token 已过期"),
    LOGIN_FAILED(40102, "邮箱或密码错误"),

    // 403 - Forbidden
    FORBIDDEN(40301, "无权访问此资源"),

    // 404 - Not Found
    USER_NOT_FOUND(40401, "用户不存在"),
    CATEGORY_NOT_FOUND(40402, "分类不存在"),
    BILL_NOT_FOUND(40403, "账单不存在"),

    // 500 - Internal Server Error
    INTERNAL_ERROR(50001, "服务器内部错误"),
    FEIGN_CALL_FAILED(50002, "微服务调用失败"),
    AI_CATEGORIZE_FAILED(50002, "AI 自动分类失败，请重试"),
    AI_ANALYSIS_FAILED(50003, "AI 消费分析失败，请重试"),
    AI_REPORT_FAILED(50004, "AI 财务报告生成失败，请重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
