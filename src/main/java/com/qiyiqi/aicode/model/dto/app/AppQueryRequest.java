package com.qiyiqi.aicode.model.dto.app;

import com.qiyiqi.aicode.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用查询请求
 *
 * @author qiyiqi
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

    /**
     * id（雪花 id 为 18~19 位，超出前端安全整数范围，统一按字符串接收）
     */
    private String id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户 id（雪花 id 为 18~19 位，超出前端安全整数范围，统一按字符串接收）
     */
    private String userId;

    private static final long serialVersionUID = 1L;
}
