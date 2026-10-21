package com.qiyiqi.aicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.qiyiqi.aicode.mapper")
public class AicodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AicodeApplication.class, args);
    }

}
