//package com.pictionary.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
///**
// * Web配置类
// *
// * * WebConfig类用于配置Web相关的设置。
// *  * 该类通常用于定义Spring Boot应用中的Web配置，如拦截器、视图解析器、静态资源处理等。
// *  * 通过继承`WebMvcConfigurer`接口或使用`@Configuration`注解，可以自定义Web行为。
// *  *
// *  * 示例配置：
// *  * - 添加拦截器：通过`addInterceptors`方法注册自定义拦截器。
// *  * - 配置视图解析器：通过`configureViewResolvers`方法设置视图解析器。
// *  * - 静态资源处理：通过`addResourceHandlers`方法配置静态资源路径。
// *  *
// *  * 目前该类为空，可以根据需要添加相关配置方法。
// */
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    /**
//     * 配置CORS跨域请求
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/api/**")
//                .allowedOrigins("*")
//                .allowedMethods("GET", "POST", "PUT", "DELETE")
//                .allowedHeaders("*")
//                .maxAge(3600);
//    }
//
//    /**
//     * 配置静态资源处理
//     */
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/**")
//                .addResourceLocations("classpath:/static/webpages");//
//    }
//}