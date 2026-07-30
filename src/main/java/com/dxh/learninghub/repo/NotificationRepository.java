package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Notification;
import com.dxh.learninghub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Notification> findByUser(User user, Pageable pageable);

    long countByUserAndIsReadFalse(User user);

    Optional<Notification> findByIdAndUser(Long id, User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user = :user and n.isRead = false")
    int markAllAsRead(@Param("user") User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Notification n
            where n.createdAt < :expiredAt
            """)
    int deleteAllExpiredBefore(@Param("expiredAt") LocalDateTime expiredAt);
}
