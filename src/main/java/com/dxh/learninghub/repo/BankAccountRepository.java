package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findAllByTeacherIdAndActiveTrue(Long teacherId);

    Optional<BankAccount> findByIdAndTeacherId(Long id, Long teacherId);
}
