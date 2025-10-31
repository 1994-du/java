package com.springbootproject.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket端点配置类，用于注册和启用@ServerEndpoint注解的WebSocket端点
 */
@Configuration
public class WebSocketEndpointConfig {

    /**
     * 注册一个ServerEndpointExporter，它会自动扫描并注册使用@ServerEndpoint注解的WebSocket端点
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}