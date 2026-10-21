package com.qiyiqi.aicode.ai;

import com.qiyiqi.aicode.ai.model.HtmlCodeResult;
import com.qiyiqi.aicode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {
    /**
     * 根据用户输入生成HTML代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);


    /**
     * 根据用户输入生成多文件代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);


    /**
     * 根据用户输入生成HTML代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/html-system-prompt.txt")
   Flux<String> generateHtmlCodeStream(String userMessage);


    /**
     * 根据用户输入生成多文件代码
     * @param userMessage
     * @return AI输出结果
     */

    @SystemMessage(fromResource = "prompt/multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);
}
