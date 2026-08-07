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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Query("""
            select count(p.id)
            from Payment p
            where p.status = :status
            """)
    Long countByStatusForAdmin(@Param("status") PaymentStatus status);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.status = :status
            """)
    BigDecimal sumAmountByStatusForAdmin(@Param("status") PaymentStatus status);

    @Query("""
            select coalesce(sum(p.pointsReceived), 0)
            from Payment p
            where p.status = :status
            """)
    Long sumPointsByStatusForAdmin(@Param("status") PaymentStatus status);

    @Query("""
            select month(p.paidAt), coalesce(sum(p.amount), 0),
                   coalesce(sum(p.pointsReceived), 0), count(p.id)
            from Payment p
            where p.status = com.dxh.learninghub.enums.PaymentStatus.COMPLETED
              and p.paidAt is not null
              and year(p.paidAt) = :year
            group by month(p.paidAt)
            order by month(p.paidAt)
            """)
    List<Object[]> sumCompletedPaymentsGroupByMonthForAdmin(@Param("year") Integer year);

    @Query("""
            select day(p.paidAt), coalesce(sum(p.amount), 0),
                   coalesce(sum(p.pointsReceived), 0), count(p.id)
            from Payment p
            where p.status = com.dxh.learninghub.enums.PaymentStatus.COMPLETED
              and p.paidAt is not null
              and year(p.paidAt) = :year
              and month(p.paidAt) = :month
            group by day(p.paidAt)
            order by day(p.paidAt)
            """)
    List<Object[]> sumCompletedPaymentsGroupByDayOfMonthForAdmin(
            @Param("year") Integer year,
            @Param("month") Integer month);

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
