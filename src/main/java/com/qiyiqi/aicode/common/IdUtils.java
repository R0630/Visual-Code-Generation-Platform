package com.qiyiqi.aicode.common;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.qiyiqi.aicode.exception.ErrorCode;
import com.qiyiqi.aicode.exception.ThrowUtils;

/**
 * id 解析工具类
 * <p>
 * 雪花 id 为 18~19 位，超出前端安全整数范围（Number.MAX_SAFE_INTEGER 仅 16 位），
 * 按数字传输会被截断，因此接口统一以字符串接收 id，在服务端再转换为 long。
 *
 * @author qiyiqi
 */
public final class IdUtils {

    private IdUtils() {
    }

    /**
     * 解析必填的字符串 id
     *
     * @param id        字符串 id
     * @param fieldName 字段名称，用于错误提示
     * @return 合法的 long id
     */
    public static long parseId(String id, String fieldName) {
        ThrowUtils.throwIf(StrUtil.isBlank(id) || !NumberUtil.isLong(id),
                ErrorCode.PARAMS_ERROR, fieldName + " 不能为空且必须为数字");
        long value = Long.parseLong(id);
        ThrowUtils.throwIf(value <= 0, ErrorCode.PARAMS_ERROR, fieldName + " 必须为正整数");
        return value;
    }

    /**
     * 解析可选的字符串 id（用于查询条件）
     * <p>
     * 查询场景与必填场景不同：Knife4j 默认会把数值字段填成 0，
     * 这里的 0 表示「不按该条件筛选」而不是「查 id=0 的记录」。
     *
     * @param id        字符串 id，为空或 0 表示不参与过滤
     * @param fieldName 字段名称，用于错误提示
     * @return 不参与过滤返回 null，否则返回合法的 long id
     */
    public static Long parseIdOrNull(String id, String fieldName) {
        if (StrUtil.isBlank(id)) {
            return null;
        }
        ThrowUtils.throwIf(!NumberUtil.isLong(id), ErrorCode.PARAMS_ERROR, fieldName + " 必须为数字");
        long value = Long.parseLong(id);
        // 0 / 负数视为未指定该条件
        return value <= 0 ? null : value;
    }
}
