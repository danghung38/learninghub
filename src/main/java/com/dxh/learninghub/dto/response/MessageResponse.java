package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(
    Long id,

    Long conversationId,

    Long senderId,

    String senderName,

    String content,

    LocalDateTime createdAt
) {}
