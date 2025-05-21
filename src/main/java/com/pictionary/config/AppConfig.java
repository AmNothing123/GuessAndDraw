package com.pictionary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 应用程序配置类
 * 用于配置各种Bean
 */
@Configuration
public class AppConfig {

    /**
     * 创建RestTemplate Bean
     * 用于发送HTTP请求到第三方API
     *
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}