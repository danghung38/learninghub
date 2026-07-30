package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Conversation;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.ConversationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            select c from Conversation c
            where c.type = :type
            and c.initiator.id <> c.partner.id
            and ((c.initiator = :u1 and c.partner = :u2) or (c.initiator = :u2 and c.partner = :u1))
            and (:courseId is null or c.course.id = :courseId)
            """)
    Optional<Conversation> findExisting(@Param("type") ConversationType type, @Param("u1") User u1, @Param("u2") User u2, @Param("courseId") Long courseId);

    @EntityGraph(attributePaths = {"initiator", "partner", "course"})
    @Query("""
            select c from Conversation c
            where (c.initiator = :user or c.partner = :user)
            and c.initiator.id <> c.partner.id
            order by c.updatedAt desc
            """)
    List<Conversation> findAllByParticipant(@Param("user") User user);

    @EntityGraph(attributePaths = {"initiator", "partner", "course"})
    @Query("""
            select c from Conversation c
            where c.id = :id
            and c.initiator.id <> c.partner.id
            and (c.initiator.id = :userId or c.partner.id = :userId)
            """)
    Optional<Conversation> findByIdAndParticipant(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"initiator", "partner", "course"})
    @Query("""
            select c from Conversation c
            where c.id = :id
            and c.initiator.id <> c.partner.id
            and (c.initiator.id = :userId or c.partner.id = :userId)
            """)
    Optional<Conversation> findByIdAndParticipantForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Conversation c " +
            "WHERE c.id = :conversationId " +
            "AND c.initiator.id <> c.partner.id " +
            "AND (c.initiator.username = :username OR c.partner.username = :username)")
    boolean existsByIdAndParticipantUsername(@Param("conversationId") Long conversationId,
                                             @Param("username") String username);
}
