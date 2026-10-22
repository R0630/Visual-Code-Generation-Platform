package com.qiyiqi.aicode.constant;

import java.util.Set;

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

    /**
     * 允许作为排序字段的列名白名单
     * <p>
     * 排序字段会拼接进 SQL，必须做白名单校验，防止 SQL 注入。
     */
    Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "appName", "cover", "initPrompt", "codeGenType", "deployKey",
            "deployedTime", "priority", "userId", "editTime", "createTime", "updateTime"
    );

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";


}
