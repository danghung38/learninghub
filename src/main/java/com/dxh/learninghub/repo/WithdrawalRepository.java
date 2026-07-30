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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"teacher", "bankAccount"})
    @Query("select w from Withdrawal w where w.id = :id")
    Optional<Withdrawal> findByIdForUpdate(@Param("id") Long id);
}
