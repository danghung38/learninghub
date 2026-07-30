package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.PointTransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "point_transactions",
        indexes = {
                @Index(
                        name = "idx_point_transaction_user_created",
                        columnList = "user_id, create_at"),
                @Index(
                        name = "idx_point_transaction_type_created",
                        columnList = "transaction_type, create_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PointTransaction extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "points", nullable = false)
    Long points;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    PointTransactionType transactionType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", unique = true)
    Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    Course course;

    @Column(name = "description")
    String description;
}
