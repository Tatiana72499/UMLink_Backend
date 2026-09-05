package com.examensw1.umlcollab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocketMessageBroker @RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthenticationInterceptor webSocketAuthenticationInterceptor;
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) { registry.enableSimpleBroker("/topic"); registry.setApplicationDestinationPrefixes("/app"); }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) { registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:3000", "http://localhost:4200", "http://localhost:5173"); }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) { registration.interceptors(webSocketAuthenticationInterceptor); }
}
