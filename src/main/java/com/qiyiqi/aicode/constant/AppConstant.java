package com.qiyiqi.aicode.constant;

/**
 * 应用常量
 *
 * @author qiyiqi
 */
public interface AppConstant {

    /**
     * 用户侧分页查询时，每页最大条数
     */
    int PAGE_SIZE_MAX = 20;

    /**
     * 应用名称最大长度
     */
    int APP_NAME_MAX_LENGTH = 80;

    /**
     * 未填写应用名称时，默认截取 initPrompt 的长度
     */
    int DEFAULT_APP_NAME_LENGTH = 12;

    /**
     * 精选应用的最小优先级（priority >= 该值即视为精选应用）
     */
    int GOOD_APP_MIN_PRIORITY = 1;

}
