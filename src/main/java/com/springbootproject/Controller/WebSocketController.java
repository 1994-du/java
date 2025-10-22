package com.springbootproject.Controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * WebSocket控制器，用于处理WebSocket消息
 */
@Controller
public class WebSocketController {

    /**
     * 处理从客户端发送到/app/hello的消息
     * 消息将被广播到所有订阅/topic/greetings的客户端
     */
    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage message) {
        // 创建问候消息并返回
        return new Greeting("Hello, " + message.getName() + "!");
    }

    /**
     * 用于接收客户端消息的简单数据类
     */
    public static class HelloMessage {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 用于发送给客户端的问候消息类
     */
    public static class Greeting {
        private String content;

        public Greeting(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}