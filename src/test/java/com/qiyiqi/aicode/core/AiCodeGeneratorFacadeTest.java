package com.qiyiqi.aicode.core;

import com.qiyiqi.aicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;


import java.util.List;


@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;



    @Test
    void GenerateAndSaveCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("生成一个登录页面", CodeGenTypeEnum.MULTI_FILE);

       //阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        //拼接字符串得到完整内容
        String completeCode = String.join("", result);
        Assertions.assertNotNull(completeCode);
    }
    @Test
    void GenerateAndSaveCodeStream1() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("生成一个个人主页界面，不超过50行", CodeGenTypeEnum.HTML);

        //阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        //拼接字符串得到完整内容
        String completeCode = String.join("", result);
        Assertions.assertNotNull(completeCode);
    }
}