package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "messages",
        indexes = @Index(
                name = "idx_message_conversation_created",
                columnList = "conversation_id, create_at"))
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Message extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    MessageType type;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    String content;
}
