package com.springbootproject.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源映射，让/uploads/**路径可以访问到uploads目录下的文件
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}