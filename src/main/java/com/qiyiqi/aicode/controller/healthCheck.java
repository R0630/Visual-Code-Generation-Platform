package com.qiyiqi.aicode.controller;

import com.qiyiqi.aicode.common.BaseResponse;
import com.qiyiqi.aicode.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class healthCheck {
    @GetMapping("/")
    public BaseResponse<String> health(){
        return ResultUtils.success("ok");
    }
}
