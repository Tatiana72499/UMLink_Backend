package com.examensw1.umlcollab.config;

import com.examensw1.umlcollab.features.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {

    private final AuthService authService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Autenticación WebSocket requerida.");
        }

        try {
            String email = authService.subject(authorization.substring(7));
            accessor.setUser(new UsernamePasswordAuthenticationToken(email, null));
            return message;
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("Token WebSocket inválido.");
        }
    }
}
