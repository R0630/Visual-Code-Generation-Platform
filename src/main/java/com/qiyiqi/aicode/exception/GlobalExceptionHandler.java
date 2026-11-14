package com.qiyiqi.aicode.exception;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qiyiqi.aicode.common.BaseResponse;
import com.qiyiqi.aicode.common.ResultUtils;
import dev.langchain4j.exception.LangChain4jException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(LangChain4jException.class)
    public BaseResponse<?> langChain4jExceptionHandler(LangChain4jException e) {
        log.error("LangChain4jException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, extractFriendlyAiMessage(e));
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 从 langchain4j 异常中提取对用户友好的中文提示。
     * langchain4j 异常的 message 通常是模型供应商返回的原始 JSON，例如：
     * {"error":{"message":"Insufficient Balance ...","type":"insufficient_balance_error",...}}
     */
    public static String extractFriendlyAiMessage(Throwable error) {
        String message = error.getMessage();
        if (StrUtil.isBlank(message)) {
            return "AI 服务暂时不可用，请稍后重试";
        }
        String lower = message.toLowerCase();
        if (lower.contains("insufficient balance") || lower.contains("insufficient_balance")) {
            return "AI 服务余额不足，请充值后重试";
        }
        if (lower.contains("rate limit") || lower.contains("rate_limit")) {
            return "AI 服务请求过于频繁，请稍后重试";
        }
        if (lower.contains("authentication") || lower.contains("invalid_api_key") || lower.contains("unauthorized")) {
            return "AI 服务鉴权失败，请联系管理员检查配置";
        }
        // 尝试从 JSON 中提取 error.message
        try {
            JSONObject json = JSONUtil.parseObj(message);
            JSONObject errorObj = json.getJSONObject("error");
            if (errorObj != null) {
                String apiMessage = errorObj.getStr("message");
                if (StrUtil.isNotBlank(apiMessage)) {
                    return "AI 服务异常：" + apiMessage;
                }
            }
        } catch (Exception ignored) {
            // message 不是 JSON，走兜底
        }
        return "AI 服务异常：" + message;
    }
}
