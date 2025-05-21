package com.pictionary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

//springboot主程序必须加上@SpringBootApplication注解
/**
 * Spring Boot 应用的主配置注解，排除数据库自动配置。
 * 使用 exclude 属性来排除不需要的自动配置类，这里排除了 DataSourceAutoConfiguration，
 * 表示不启用与数据库相关的自动配置。这在项目不使用数据库时非常有用，
 * 可以避免因缺少数据库连接配置而导致的启动错误。
 */

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})public class PictionaryApplication {
    //主入口，运行main即启动服务器
    public static void main(String[] args) {
        SpringApplication.run(PictionaryApplication.class, args);
    }

}
