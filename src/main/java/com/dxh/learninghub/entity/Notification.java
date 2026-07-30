package com.dxh.learninghub.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(
                        name = "idx_notification_user_created",
                        columnList = "user_id, create_at"),
                @Index(
                        name = "idx_notification_user_read",
                        columnList = "user_id, is_read")
        })
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends AbstractEntity<Long>{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    User user;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "message", nullable = false, length = 2000)
    String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    Boolean isRead = false;

    @Column(name = "url")
    String url;

    @Column(name = "avatarUrl")
    String avatarUrl;

    @PrePersist
    private void prePersist() {
        if (isRead == null) {
            isRead = false;
        }
    }
}
