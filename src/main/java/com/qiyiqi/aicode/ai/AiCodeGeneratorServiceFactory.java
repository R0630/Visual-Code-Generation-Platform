package com.qiyiqi.aicode.ai;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qiyiqi.aicode.ai.tools.FileWriteTool;
import com.qiyiqi.aicode.exception.BusinessException;
import com.qiyiqi.aicode.exception.ErrorCode;
import com.qiyiqi.aicode.model.enums.CodeGenTypeEnum;
import com.qiyiqi.aicode.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI服务创建工厂
 *
 * chatMemoryProvider 策略说明：
 * - 每次调用都新建 MessageWindowChatMemory，langchain4j 自动通过 Redis store 恢复该 appId 的历史消息
 * - 如果 Redis store 为空（第一次对话 / Redis 被清空），再从业务数据库（chat_history 表）加载历史
 * - Vue 项目用 100 条窗口（工具调用多），HTML/MULTI_FILE 用 50 条
 * - 限制 Vue 项目单次对话最多 20 次工具调用（maxSequentialToolsInvocations），防止推理模型无限循环
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {
    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * AI 服务实例缓存（可选，缓存减少重复创建开销）
     * 注意：即使缓存了 service，chatMemoryProvider 每次被调用时仍会新建 memory 实例并从 Redis store 恢复历史，
     * 所以不会出现记忆丢失问题。
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    // VUE_PROJECT 工具调用多，窗口设为 100
                    .chatMemoryProvider(memoryId -> buildMemory((Long) memoryId, 100))
                    .tools(new FileWriteTool())
                    // 硬限制：单次对话最多 20 次工具调用，防止推理模型无限循环写入
                    .maxSequentialToolsInvocations(20)
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))
                    .build();

            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    // HTML/MULTI_FILE 无工具调用，窗口 50 足够
                    .chatMemoryProvider(memoryId -> buildMemory((Long) memoryId, 50))
                    .build();

            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 构建带持久化恢复能力的 ChatMemory：
     * 1. 先通过 Redis store 恢复历史（langchain4j 自动完成）
     * 2. 如果 Redis 为空，再从业务数据库加载历史
     */
    private MessageWindowChatMemory buildMemory(long appId, int maxMessages) {
        MessageWindowChatMemory memory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(maxMessages)
                .build();
        // 如果 Redis store 里没有历史（第一次对话或 Redis 被清空），从业务数据库加载
        if (memory.messages().isEmpty()) {
            chatHistoryService.loadChatHistoryToMemory(appId, memory, maxMessages);
        }
        return memory;
    }

    /** 默认提供一个 Bean */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L, CodeGenTypeEnum.HTML);
    }
}
