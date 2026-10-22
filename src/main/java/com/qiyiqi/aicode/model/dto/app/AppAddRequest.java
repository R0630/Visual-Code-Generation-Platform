package com.qiyiqi.aicode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 *
 * @author qiyiqi
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用名称（可选，未填写时默认取 initPrompt 的前若干字符）
     */
    private String appName;

    /**
     * 应用初始化的 prompt（必填）
     */
    private String initPrompt;

    private static final long serialVersionUID = 1L;
}
