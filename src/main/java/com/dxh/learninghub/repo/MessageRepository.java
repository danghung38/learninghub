package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    /**
     * CẢNH BÁO: không tự verify quyền truy cập.
     * Phải gọi sau khi đã xác thực participant qua findByIdAndParticipant.
     */
    @EntityGraph(attributePaths = "sender")
    Page<Message> findByConversationId(Long conversationId, Pageable pageable);

    @Query("""
            select m from Message m
            where m.id in (
                select max(m2.id) from Message m2
                where m2.conversation.id in :conversationIds
                group by m2.conversation.id
            )
            """)
    List<Message> findLastMessagesByConversationIds(@Param("conversationIds") List<Long> conversationIds);
}