package com.springbootproject.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类，启用WebSocket消息代理
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，用于将消息广播到客户端
        config.enableSimpleBroker("/topic", "/queue");
        // 设置应用程序目的地前缀，客户端发送消息时需要使用这个前缀
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点，客户端将使用这个端点连接到服务器
        // withSockJS()提供了对不支持WebSocket的浏览器的兼容性支持
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*") // 允许所有来源的连接，可以根据需要限制
                .withSockJS();
    }
}