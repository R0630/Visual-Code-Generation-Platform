package com.qiyiqi.aicode.common;

import cn.hutool.core.util.StrUtil;

/**
 * 查询条件归一化工具
 *
 * @author qiyiqi
 */
public final class QueryUtils {

    private QueryUtils() {
    }

    /**
     * 空字符串转 null
     * <p>
     * MyBatis-Flex 默认只忽略 null 值，空字符串会生成 {@code cover = ''} 这样的条件，
     * 从而匹配不到库中为 NULL 的记录，导致分页结果为空。
     * 前端（含 Knife4j 默认示例）常把未填写的字段传成空字符串，因此查询前需统一归一化。
     *
     * @param value 原始查询值
     * @return 空字符串返回 null，否则返回原值
     */
    public static String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value;
    }
}
