package com.springbootproject.Config;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket配置类
 */
@Component
public class WebSocketConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 获取请求参数
        List<String> usernameList = request.getParameterMap().get("username");
        String username = usernameList != null && !usernameList.isEmpty() ? usernameList.get(0) : "Anonymous";
        
        // 将用户名保存到WebSocket会话中
        sec.getUserProperties().put("username", username);
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return super.getEndpointInstance(endpointClass);
    }
}