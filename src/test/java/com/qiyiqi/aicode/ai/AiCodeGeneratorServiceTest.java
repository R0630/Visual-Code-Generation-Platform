package com.qiyiqi.aicode.ai;

import com.qiyiqi.aicode.ai.model.HtmlCodeResult;
import com.qiyiqi.aicode.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class AiCodeGeneratorServiceTest {
    @Resource
    private  AiCodeGeneratorService aiCodeGeneratorService ;
/**
 * 测试方法：生成HTML代码
 * 该方法用于测试生成HTML代码的功能
 * 使用JUnit测试框架进行测试
 */
    @Test
    void generateHtmlCode() {  // 测试生成HTML代码的方法
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("帮我做一个个人博客，不超过20行"); // 调用生成HTML代码的方法
        Assertions.assertNotNull(result); // 断言结果不为空

    }

    @Test
    void generateMutiFileCode() {

        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("帮我做一个可爱风格的留言板，不超过50行"); // 调用生成HTML代码的方法
        Assertions.assertNotNull(result); // 断言结果不为空

    }
}