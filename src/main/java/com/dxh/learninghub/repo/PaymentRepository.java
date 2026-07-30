package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Payment;
import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.user where p.merchantTransactionRef = :transactionRef")
    Optional<Payment> findByTransactionRefForUpdate(@Param("transactionRef") String transactionRef);

    @EntityGraph(attributePaths = "user")
    Optional<Payment> findByMerchantTransactionRefAndUserId(
            String merchantTransactionRef,
            Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Payment> findByMerchantTransactionRef(String merchantTransactionRef);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select p from Payment p
            where (:userId is null or p.user.id = :userId)
              and (:status is null or p.status = :status)
              and (:method is null or p.paymentMethod = :method)
              and (:fromDate is null or p.createdAt >= :fromDate)
              and (:toDate is null or p.createdAt < :toDate)
            """)
    Page<Payment> findPayments(
            @Param("userId") Long userId,
            @Param("status") PaymentStatus status,
            @Param("method") PaymentMethod method,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
            set p.status = com.dxh.learninghub.enums.PaymentStatus.EXPIRED,
                p.updatedAt = :now
            where p.status = com.dxh.learninghub.enums.PaymentStatus.PENDING
              and p.expiresAt <= :now
            """)
    int expirePendingPayments(@Param("now") LocalDateTime now);
}
