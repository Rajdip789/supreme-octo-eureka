package com.rajdip14.game.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.util.List;

import static com.rajdip14.game.utils.AppConstants.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(BROKER_QUEUE_PREFIX);
        config.setApplicationDestinationPrefixes(APP_PATH);
        config.setUserDestinationPrefix(BROKER_USER_PREFIX); //by default it's /user
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(SOCKET_PATH)
                .setAllowedOriginPatterns(allowedOrigins[0]);
    }
}