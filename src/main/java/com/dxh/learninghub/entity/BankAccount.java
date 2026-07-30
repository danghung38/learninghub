package com.dxh.learninghub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BankAccount extends AbstractEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    User teacher;

    @Column(name = "bank_name", nullable = false, length = 100)
    String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 150)
    String accountHolder;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    Boolean isDefault = false;

    @Builder.Default
    @Column(name = "active", nullable = false)
    Boolean active = true;

    @PrePersist
    private void prePersist() {
        if (isDefault == null) {
            isDefault = false;
        }
        if (active == null) {
            active = true;
        }
    }
}
