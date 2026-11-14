package com.qiyiqi.aicode.core;


import cn.hutool.json.JSONUtil;
import com.qiyiqi.aicode.ai.AiCodeGeneratorService;
import com.qiyiqi.aicode.ai.AiCodeGeneratorServiceFactory;
import com.qiyiqi.aicode.ai.model.message.AiResponseMessage;
import com.qiyiqi.aicode.ai.model.message.ToolExecutedMessage;
import com.qiyiqi.aicode.ai.model.message.ToolRequestMessage;
import com.qiyiqi.aicode.core.parser.CodeParserExecutor;
import com.qiyiqi.aicode.core.saver.CodeFileSaverExecutor;
import com.qiyiqi.aicode.exception.BusinessException;
import com.qiyiqi.aicode.exception.ErrorCode;
import com.qiyiqi.aicode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;



import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {


    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;



//    /**
//     * 统一入口：根据类型生成并保存代码
//     *
//     * @param userMessage     用户提示词
//     * @param codeGenTypeEnum 生成类型
//     * @return 保存的目录
//     */
//    public Flux<String> generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
//
//        // 根据 appId 获取对应的 AI 服务实例
//        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory
//                .getAiCodeGeneratorService(appId, codeGenTypeEnum);
//        return switch (codeGenTypeEnum) {
//            case HTML -> {
//                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
//            }
//            case MULTI_FILE -> {
//                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
//            }
//            case VUE_PROJECT -> {
//                Flux<String> codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
//                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
//            }
//            default -> {
//                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
//            }
//        };
//    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId) {

        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory
                .getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(appId, userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(appId, userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                // VUE_PROJECT 使用推理模型 + FileWriteTool（工具调用），AI 自行将文件写入磁盘
                // 不走 processCodeStream 的纯文本解析/保存流程，直接返回流给 JsonMessageStreamHandler 处理
                yield aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };

    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType,Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {

            // 流式返回完成后保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType,appId);
                log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */

    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream
                    .onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    // 在 1.1.0 版本中，使用 onToolExecuted 来获取工具调用信息
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        // 从 ToolExecution 中提取 ToolExecutionRequest
                        ToolExecutionRequest request = toolExecution.request();

                        // 1. 构建并推送工具请求消息 (因为它在执行后触发，所以此时才推送)
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(request);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));

                        // 2. 同时推送工具执行结果消息
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> sink.complete())
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

}
