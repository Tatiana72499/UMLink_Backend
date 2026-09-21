package com.examensw1.umlcollab.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocketMessageBroker @RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthenticationInterceptor webSocketAuthenticationInterceptor;
    @Value("${app.cors.allowed-origins}") private List<String> allowedOrigins;
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) { registry.enableSimpleBroker("/topic"); registry.setApplicationDestinationPrefixes("/app"); }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) { registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new)); }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) { registration.interceptors(webSocketAuthenticationInterceptor); }
}
