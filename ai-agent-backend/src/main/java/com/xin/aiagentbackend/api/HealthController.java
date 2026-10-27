package com.xin.aiagentbackend.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器（Controller）。
 * <p>对外暴露服务健康检查接口（路径 /health），供探活与运维监控使用。</p>
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /** 健康检查：服务可用时返回 ok */
    @GetMapping
    public String healthCheck() {
        return "ok";
    }
}
