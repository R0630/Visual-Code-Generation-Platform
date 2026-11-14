package com.qiyiqi.aicode.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;


@SpringBootTest
class AiCodeGeneratorServiceTest {
    @Resource
    private  AiCodeGeneratorService aiCodeGeneratorService ;
/**
 * 测试方法：生成HTML代码（流式）
 * 该方法用于测试生成HTML代码的功能
 * 使用JUnit测试框架进行测试
 */
    @Test
    void generateHtmlCodeStream() {  // 测试生成HTML代码的方法
        Flux<String> stream = aiCodeGeneratorService.generateHtmlCodeStream(1L, "帮我做一个个人博客，不超过20行");
        List<String> chunks = stream.collectList().block();
        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(chunks.isEmpty());
    }

    @Test
    void generateMultiFileCodeStream() {
        Flux<String> stream = aiCodeGeneratorService.generateMultiFileCodeStream(1L, "帮我做一个可爱风格的留言板，不超过50行");
        List<String> chunks = stream.collectList().block();
        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(chunks.isEmpty());
    }

    @Test
    void testChatMemory() {
        Flux<String> stream1 = aiCodeGeneratorService.generateHtmlCodeStream(1L, "做个程序员鱼皮的工具网站，总代码量不超过 20 行");
        List<String> chunks1 = stream1.collectList().block();
        Assertions.assertNotNull(chunks1);

        Flux<String> stream2 = aiCodeGeneratorService.generateHtmlCodeStream(1L, "不要生成网站，告诉我你刚刚做了什么？");
        List<String> chunks2 = stream2.collectList().block();
        Assertions.assertNotNull(chunks2);
    }

}
