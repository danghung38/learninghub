package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Withdrawal;
import com.dxh.learninghub.enums.WithdrawalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    @EntityGraph(attributePaths = "bankAccount")
    Page<Withdrawal> findAllByTeacherIdOrderByCreatedAtDesc(
            Long teacherId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"teacher", "bankAccount"})
    @Query("""
            select w from Withdrawal w
            where (:status is null or w.status = :status)
            """)
    Page<Withdrawal> findAllByStatus(@Param("status") WithdrawalStatus status, Pageable pageable);

    @Query("""
            select count(w.id)
            from Withdrawal w
            where w.status = :status
            """)
    Long countByStatusForAdmin(@Param("status") WithdrawalStatus status);

    @Query("""
            select coalesce(sum(w.amount), 0)
            from Withdrawal w
            where w.status = :status
            """)
    BigDecimal sumAmountByStatusForAdmin(@Param("status") WithdrawalStatus status);

    @Query("""
            select coalesce(sum(w.points), 0)
            from Withdrawal w
            where w.status = :status
            """)
    Long sumPointsByStatusForAdmin(@Param("status") WithdrawalStatus status);

    @Query("""
            select month(w.updatedAt), coalesce(sum(w.amount), 0),
                   coalesce(sum(w.points), 0), count(w.id)
            from Withdrawal w
            where w.status = com.dxh.learninghub.enums.WithdrawalStatus.PAID
              and w.updatedAt is not null
              and year(w.updatedAt) = :year
            group by month(w.updatedAt)
            order by month(w.updatedAt)
            """)
    List<Object[]> sumPaidWithdrawalsGroupByMonthForAdmin(@Param("year") Integer year);

    @Query("""
            select day(w.updatedAt), coalesce(sum(w.amount), 0),
                   coalesce(sum(w.points), 0), count(w.id)
            from Withdrawal w
            where w.status = com.dxh.learninghub.enums.WithdrawalStatus.PAID
              and w.updatedAt is not null
              and year(w.updatedAt) = :year
              and month(w.updatedAt) = :month
            group by day(w.updatedAt)
            order by day(w.updatedAt)
            """)
    List<Object[]> sumPaidWithdrawalsGroupByDayOfMonthForAdmin(
            @Param("year") Integer year,
            @Param("month") Integer month);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"teacher", "bankAccount"})
    @Query("select w from Withdrawal w where w.id = :id")
    Optional<Withdrawal> findByIdForUpdate(@Param("id") Long id);
}
