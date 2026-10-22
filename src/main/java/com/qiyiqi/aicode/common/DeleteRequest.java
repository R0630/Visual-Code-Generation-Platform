package com.qiyiqi.aicode.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable {

    /**
     * ID
     * <p>
     * 雪花 id 为 18~19 位，超出前端安全整数范围（Number.MAX_SAFE_INTEGER 仅 16 位），
     * 按数字传输会被截断，故统一按字符串接收。
     */
    private String id;

    private static final long serialVersionUID = 1L;
}
