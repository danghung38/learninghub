package com.dxh.learninghub.controller;

import com.dxh.learninghub.dto.request.ChatSendRequest;
import com.dxh.learninghub.service.interfac.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Chat WebSocket", description = "WebSocket operations for real-time chat messages")
public class ChatWebSocketController {

    ChatService chatService;

    @Operation(summary = "Send a chat message", description = "Send an authenticated message to a conversation over WebSocket")
    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload ChatSendRequest request, JwtAuthenticationToken authentication) {
        String username = authentication.getToken().getSubject();
        chatService.sendMessage(username, request);
    }
}
