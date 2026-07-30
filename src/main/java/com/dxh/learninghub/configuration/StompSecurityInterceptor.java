package com.dxh.learninghub.configuration;

import com.dxh.learninghub.repo.ConversationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StompSecurityInterceptor implements ChannelInterceptor {

    static String CONVERSATION_TOPIC = "/topic/conversation/";

    CustomJwtDecoder customJwtDecoder;
    ConversationRepository conversationRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(accessor);
            case SEND -> authorizeSend(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization =
                accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing or incorrect Authorization header");
        }

        try {
            String token = authorization.substring(7);
            Jwt jwt = customJwtDecoder.decode(token);

            accessor.setUser(new JwtAuthenticationToken(jwt));
        } catch (Exception exception) {
            throw new BadCredentialsException("Invalid WebSocket token", exception);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null) {
            throw new AccessDeniedException("Invalid destination");
        }

        // Kênh riêng của chính user:
        // /user/queue/notifications
        if (destination.startsWith("/user/")) {
            requireAuthenticated(accessor);
            return;
        }

        // Chỉ kiểm tra quyền đặc biệt với conversation.
        if (destination.startsWith(CONVERSATION_TOPIC)) {
            Principal user = requireAuthenticated(accessor);
            Long conversationId = extractConversationId(destination);

            boolean allowed = conversationRepository.existsByIdAndParticipantUsername(conversationId, user.getName());

            if (!allowed) {
                throw new AccessDeniedException("You don't belong in this conversation");
            }

            return;
        }

        throw new AccessDeniedException("Subscribe to this destination is not allowed");
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        /*
         * Client chỉ được gửi vào /app/**
         * Không được tự gửi thẳng vào:
         * /topic/**
         * /queue/**
         */
        if (destination == null || !destination.startsWith("/app/")) {
            throw new AccessDeniedException("Clients are only allowed to send messages to /app/**");
        }

        requireAuthenticated(accessor);
    }

    private Principal requireAuthenticated(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();

        if (user == null) throw new AccessDeniedException("WebSocket has not been verified.");

        return user;
    }

    private Long extractConversationId(String destination) {
        try {
            return Long.parseLong(destination.substring(destination.lastIndexOf("/") + 1));
        } catch (Exception exception) {
            throw new AccessDeniedException("Invalid Conversation ID");
        }
    }
}