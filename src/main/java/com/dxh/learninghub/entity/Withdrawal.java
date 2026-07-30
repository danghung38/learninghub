package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.WithdrawalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(
        name = "withdrawals",
        indexes = {
                @Index(
                        name = "idx_withdrawal_teacher_created",
                        columnList = "teacher_id, create_at"),
                @Index(
                        name = "idx_withdrawal_status_created",
                        columnList = "status, create_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Withdrawal extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    BankAccount bankAccount;

    @Column(name = "bank_name", nullable = false, length = 100)
    String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 150)
    String accountHolder;

    @Column(name = "points", nullable = false)
    Long points;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    WithdrawalStatus status;

    @Column(name = "payment_proof_url", length = 1000)
    String paymentProofUrl;

    @Column(name = "rejection_reason", length = 500)
    String rejectionReason;
}
