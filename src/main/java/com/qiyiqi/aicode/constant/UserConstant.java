package com.qiyiqi.aicode.constant;

import java.util.Set;

/**
 * 用户常量
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    // endregion

    /**
     * 允许作为排序字段的列名白名单
     * <p>
     * 排序字段会拼接进 SQL，必须做白名单校验，防止 SQL 注入。
     */
    Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "userAccount", "userName", "userAvatar", "userProfile",
            "userRole", "editTime", "createTime", "updateTime"
    );
}