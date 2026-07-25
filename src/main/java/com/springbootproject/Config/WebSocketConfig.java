package com.springbootproject.Config;

import com.springbootproject.Util.JwtUtils;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * WebSocket配置类
 */
@Component
public class WebSocketConfig extends ServerEndpointConfig.Configurator {

    private static JwtUtils jwtUtils;

    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        WebSocketConfig.jwtUtils = jwtUtils;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 获取请求参数
        List<String> usernameList = request.getParameterMap().get("username");
        String username = usernameList != null && !usernameList.isEmpty() ? usernameList.get(0) : "Anonymous";

        sec.getUserProperties().put("username", username);

        String token = resolveToken(request);
        if (token != null) {
            sec.getUserProperties().put("authToken", token);
        }

        if (jwtUtils == null) {
            sec.getUserProperties().put("authError", "认证服务暂不可用");
            return;
        }

        if (token == null || token.isBlank()) {
            sec.getUserProperties().put("authError", "未提供登录凭证");
            return;
        }

        if (!jwtUtils.validateToken(token)) {
            sec.getUserProperties().put("authError", "登录已过期，请重新登录");
            return;
        }

        String authenticatedUsername = jwtUtils.extractUsername(token);
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            sec.getUserProperties().put("authError", "登录信息无效，请重新登录");
            return;
        }

        sec.getUserProperties().put("authUsername", authenticatedUsername);
        sec.getUserProperties().put("username", authenticatedUsername);
    }

    private String resolveToken(HandshakeRequest request) {
        if (request == null) {
            return null;
        }

        String headerToken = firstNonBlank(
                normalizeAuthorizationValue(getFirstHeader(request.getHeaders(), "Authorization")),
                getFirstHeader(request.getHeaders(), "token"),
                getFirstHeader(request.getHeaders(), "user-token"));
        if (headerToken != null) {
            return headerToken;
        }

        String paramToken = firstNonBlank(
                getFirstParameter(request.getParameterMap(), "token"),
                getFirstParameter(request.getParameterMap(), "user-token"));
        if (paramToken != null) {
            return paramToken;
        }

        return extractTokenFromCookieHeader(getFirstHeader(request.getHeaders(), "Cookie"));
    }

    private String getFirstHeader(Map<String, List<String>> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && name.equalsIgnoreCase(entry.getKey())) {
                List<String> values = entry.getValue();
                if (values != null) {
                    for (String value : values) {
                        if (value != null && !value.isBlank()) {
                            return value.trim();
                        }
                    }
                }
            }
        }
        return null;
    }

    private String getFirstParameter(Map<String, List<String>> parameters, String name) {
        if (parameters == null || name == null) {
            return null;
        }
        List<String> values = parameters.get(name);
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String extractTokenFromCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] pair = cookie.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String cookieName = pair[0].trim();
            if ("token".equals(cookieName) || "user-token".equals(cookieName)) {
                String cookieValue = pair[1].trim();
                return cookieValue.isEmpty() ? null : cookieValue;
            }
        }
        return null;
    }

    private String normalizeAuthorizationValue(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return authorizationHeader.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return super.getEndpointInstance(endpointClass);
    }
}
