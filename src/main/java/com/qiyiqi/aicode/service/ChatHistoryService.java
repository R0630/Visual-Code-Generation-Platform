package com.qiyiqi.aicode.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.qiyiqi.aicode.exception.ThrowUtils;
import com.qiyiqi.aicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.qiyiqi.aicode.model.entity.ChatHistory;
import com.qiyiqi.aicode.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author qiyiqi
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话记录
     * @param appId 应用ID
     * @param message 消息内容
     * @param messageType 消息类型
     * @param userId 用户ID
     * @return 是否添加成功
     */
    boolean addChatMessage(Long appId,String message,String messageType,Long userId);


    /**
     * 根据应用id删除对话记录
     * @param appId 应用id
     * @return 是否删除成功
     */
    boolean deleteByAppId(Long appId);

    /**
     * 根据查询条件获取查询包装器
     * @param chatHistoryQueryRequest 查询条件
     * @return 查询包装器
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     *  分页查询应用的对话历史
     * @param appId 应用id
     * @param pageSize 页面大小
     * @param lastCreateTime 游标查询 - 最后一条记录的创建时间
     * @param loginUser 登录用户
     * @return 分页数据
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);


    /**
     *  加载历史对话到记忆中
     * @param appId 应用id
     * @param chatMemory 记忆
     * @param maxCount 最大加载数量
     * @return 加载的数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
