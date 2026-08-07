package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.PointTransaction;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.PointTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    boolean existsByPaymentId(Long paymentId);

    @EntityGraph(attributePaths = {"user", "course", "payment"})
    @Query("""
            select pt from PointTransaction pt
            where pt.user = :user
              and (:type is null or pt.transactionType = :type)
            """)
    Page<PointTransaction> findMyTransactions(
            @Param("user") User user,
            @Param("type") PointTransactionType type,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "course", "payment"})
    @Query("""
            select pt from PointTransaction pt
            where (:userId is null or pt.user.id = :userId)
              and (:type is null or pt.transactionType = :type)
              and (:fromDate is null or pt.createdAt >= :fromDate)
              and (:toDate is null or pt.createdAt < :toDate)
            """)
    Page<PointTransaction> findAllTransactions(
            @Param("userId") Long userId,
            @Param("type") PointTransactionType type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
