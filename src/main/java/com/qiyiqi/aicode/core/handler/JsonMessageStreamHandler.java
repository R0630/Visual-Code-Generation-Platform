package com.qiyiqi.aicode.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qiyiqi.aicode.ai.model.message.*;
import com.qiyiqi.aicode.ai.tools.FileWriteTool;
import com.qiyiqi.aicode.constant.AppConstant;
import com.qiyiqi.aicode.core.builder.VueProjectBuilder;
import com.qiyiqi.aicode.exception.GlobalExceptionHandler;
import com.qiyiqi.aicode.model.entity.User;
import com.qiyiqi.aicode.model.enums.ChatHistoryMessageTypeEnum;
import com.qiyiqi.aicode.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 消息流处理器
 * 处理 VUE_PROJECT 类型的流式响应。
 *
 * 设计要点：
 * 1. 兼容 JSON 消息流（AI_RESPONSE / TOOL_REQUEST / TOOL_EXECUTED）和纯文本流（AiServices 直接返回）
 * 2. 自动过滤推理模型的 <think>...</think> 标签
 * 3. 通过 {@link FileWriteTool#ACTIVE_SINKS} 桥接，把工具执行事件实时合并到 SSE 流中
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    /** 推理模型的思考标签正则（DeepSeek / OpenAI o1/o3 等） */
    private static final String THINK_TAG_PATTERN = "<think>[\\s\\S]*?</think>";

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 处理流式响应（VUE_PROJECT）
     *
     * @param originFlux         原始流（可能是 JSON 消息流，也可能是纯文本流）
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流（已合并工具事件提示，已过滤 think 标签）
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // ========= 工具事件桥接 =========
        // 注册一个 Sink 到 FileWriteTool 的全局注册表，让工具执行时能推送事件到 SSE 流
        Sinks.Many<String> toolEventSink = Sinks.many().unicast().onBackpressureBuffer();
        // 如果有旧的 sink（理论上不应该），先 complete 掉避免内存泄漏
        Sinks.Many<String> oldSink = FileWriteTool.ACTIVE_SINKS.put(appId, toolEventSink);
        if (oldSink != null) {
            oldSink.tryEmitComplete();
        }

        // ========= AI 文本流处理 =========
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        StringBuilder thinkBuffer = new StringBuilder();
        AtomicBoolean inThinkTag = new AtomicBoolean(false);

        Flux<String> aiFlux = originFlux
                .map(chunk -> handleChunk(chunk, chatHistoryStringBuilder, seenToolIds, thinkBuffer, inThinkTag))
                .filter(StrUtil::isNotEmpty);

        // ========= 合并 AI 流 + 工具事件流 =========
        Flux<String> merged = Flux.merge(aiFlux, toolEventSink.asFlux());

        return merged
                .doOnComplete(() -> {
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse,
                            ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 异步构建 Vue 项目（npm install && npm run build）
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doFinally(signalType -> {
                    // 清理：移除 Sink 注册表 + 关闭 Sink
                    FileWriteTool.ACTIVE_SINKS.remove(appId, toolEventSink);
                    toolEventSink.tryEmitComplete();
                })
                .onErrorResume(error -> {
                    log.error("AI 流式回复失败", error);
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage,
                            ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    // 清理资源
                    FileWriteTool.ACTIVE_SINKS.remove(appId, toolEventSink);
                    toolEventSink.tryEmitComplete();
                    return Flux.just(GlobalExceptionHandler.extractFriendlyAiMessage(error));
                });
    }

    /**
     * 统一处理每个 chunk：
     * 1. 如果是 JSON 消息（以 '{' 开头）→ 解析为 StreamMessage 按类型分发
     * 2. 否则当作普通 AI 文本回复，过滤掉 <think> 推理标签后透传
     */
    private String handleChunk(String chunk, StringBuilder chatHistoryStringBuilder,
                               Set<String> seenToolIds, StringBuilder thinkBuffer, AtomicBoolean inThinkTag) {
        if (StrUtil.isBlank(chunk)) {
            return "";
        }
        char firstChar = chunk.charAt(0);
        if (firstChar == '{') {
            try {
                return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
            } catch (Exception e) {
                log.warn("解析 JSON chunk 失败，回退为纯文本: {}", e.getMessage());
                return handlePlainText(chunk, chatHistoryStringBuilder, thinkBuffer, inThinkTag);
            }
        }
        return handlePlainText(chunk, chatHistoryStringBuilder, thinkBuffer, inThinkTag);
    }

    /** 处理 JSON 格式的流消息（AiResponseMessage / ToolRequestMessage / ToolExecutedMessage） */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder,
                                          Set<String> seenToolIds) {
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            log.warn("未知的 JSON 消息类型，回退为纯文本");
            return chunk;
        }
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    seenToolIds.add(toolId);
                    return "\n\n[工具调用] 🛠️ 开始执行写入文件...\n\n";
                }
                return "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String suffix = FileUtil.getSuffix(relativeFilePath);
                String content = jsonObject.getStr("content");
                String result = String.format("""
                        [工具执行] ✍️ 已写入文件 %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

    /** 处理纯文本 chunk：过滤 <think> 推理标签后透传 */
    private String handlePlainText(String chunk, StringBuilder chatHistoryStringBuilder,
                                   StringBuilder thinkBuffer, AtomicBoolean inThinkTag) {
        String text = filterThinkTag(chunk, thinkBuffer, inThinkTag);
        if (StrUtil.isNotBlank(text)) {
            chatHistoryStringBuilder.append(text);
            return text;
        }
        return "";
    }

    /**
     * 过滤推理模型输出的 <think>...</think> 标签内容。
     * 标签可能被 chunk 边界截断（例如 <thi | nk>思考...</thi | nk>），
     * 因此用 thinkBuffer 做跨 chunk 缓存。
     */
    private String filterThinkTag(String chunk, StringBuilder thinkBuffer, AtomicBoolean inThinkTag) {
        String text = chunk;

        // 先尝试直接移除完整的 <think>...</think> 标签
        text = ReUtil.delAll(THINK_TAG_PATTERN, text);

        // 处理被截断的开标签 <think>
        int openIdx = text.indexOf("<think>");
        if (openIdx >= 0) {
            inThinkTag.set(true);
            thinkBuffer.setLength(0);
            thinkBuffer.append(text, openIdx + 7, text.length());
            text = text.substring(0, openIdx);
        }

        // 处理被截断的闭标签 </think>
        if (inThinkTag.get()) {
            int closeIdx = text.indexOf("</think>");
            if (closeIdx >= 0) {
                thinkBuffer.setLength(0);
                inThinkTag.set(false);
                text = text.substring(closeIdx + 8);
            } else {
                thinkBuffer.append(text);
                return "";
            }
        }

        // 处理单个 <think 开头但可能不完整的情况（安全兜底）
        if (text.contains("<think") && !text.contains("</think")) {
            inThinkTag.set(true);
            thinkBuffer.setLength(0);
            return "";
        }

        return text;
    }
}
