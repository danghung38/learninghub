package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.ConversationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Conversation extends AbstractEntity<Long> {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    ConversationType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiator_id", nullable = false)
    User initiator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    User partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    Course course;

    @Column(name = "conversation_key", length = 100, unique = true)
    String conversationKey;

    @JsonIgnore
    @Builder.Default
    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    List<Message> messages = new ArrayList<>();

    @Builder.Default
    @Column(name = "initiator_unread_count", nullable = false)
    int initiatorUnreadCount = 0;

    @Builder.Default
    @Column(name = "partner_unread_count", nullable = false)
    int partnerUnreadCount = 0;

    @PrePersist
    @PreUpdate
    void synchronizeConversationKey() {
        if (type == null || initiator == null || partner == null
                || initiator.getId() == null || partner.getId() == null
                || (course != null && course.getId() == null)) {
            return;
        }

        if (initiator.getId().equals(partner.getId())) {
            throw new IllegalStateException("Conversation participants must be different");
        }

        long firstParticipantId = Math.min(initiator.getId(), partner.getId());
        long secondParticipantId = Math.max(initiator.getId(), partner.getId());
        String courseScope = course == null ? "SUPPORT" : course.getId().toString();
        conversationKey = type.name()
                + ":" + firstParticipantId
                + ":" + secondParticipantId
                + ":" + courseScope;
    }
}