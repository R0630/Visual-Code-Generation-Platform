package com.qiyiqi.aicode.ai;

import com.qiyiqi.aicode.ai.model.HtmlCodeResult;
import com.qiyiqi.aicode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {


    /**
     * 根据用户输入生成HTML代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(@UserMessage String userMessage);


    /**
     * 根据用户输入生成多文件代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(@UserMessage String userMessage);

    /**
     * 根据用户输入生成HTML代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/html-system-prompt.txt")
   Flux<String> generateHtmlCodeStream(@MemoryId long appId, @UserMessage String userMessage);


    /**
     * 根据用户输入生成多文件代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(@MemoryId long appId, @UserMessage String userMessage);

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/vue-project-system-prompt.txt")
    Flux<String> generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);





}
