package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConversationResponse(
    Long conversationId,

    String type,

    ConversationParticipantResponse otherParticipant,

    Long courseId,

    String courseTitle,

    String lastMessage,

    LocalDateTime lastMessageAt,

    Integer unreadCount
) {}
