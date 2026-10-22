package com.qiyiqi.aicode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新请求（用户更新自己的应用，目前只支持修改应用名称）
 *
 * @author qiyiqi
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id（雪花 id 为 18~19 位，超出前端安全整数范围，统一按字符串接收）
     */
    private String id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}
