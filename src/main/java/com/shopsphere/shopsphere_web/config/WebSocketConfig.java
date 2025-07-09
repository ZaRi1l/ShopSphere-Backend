package com.shopsphere.shopsphere_web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커가 /topic으로 시작하는 주제를 구독한 클라이언트에게 메시지를 전달
        config.enableSimpleBroker("/topic");
        // 클라이언트에서 메시지를 보낼 때 /app으로 시작하는 경로로 메시지를 보냄
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 연결을 위한 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS 설정 (개발용으로 * 사용, 프로덕션에서는 구체적인 도메인 지정 필요)
                .withSockJS(); // SockJS 지원
    }
}
