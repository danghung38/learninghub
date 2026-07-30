package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.PaymentMethod;
import com.dxh.learninghub.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_user_created", columnList = "user_id, create_at"),
        @Index(name = "idx_payment_status_created", columnList = "status, create_at")
})
public class Payment extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    String merchantTransactionRef;

    @Column(name = "gateway_transaction_no", unique = true, length = 30)
    String gatewayTransactionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20)")
    PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 0)
    BigDecimal amount;

    @Column(nullable = false)
    Long pointsReceived;

    @Column(name = "response_code", length = 2)
    String responseCode;

    @Column(name = "bank_code", length = 20)
    String bankCode;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "paid_at")
    LocalDateTime paidAt;
}
