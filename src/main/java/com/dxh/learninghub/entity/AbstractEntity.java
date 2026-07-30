package com.dxh.learninghub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class AbstractEntity<T extends Serializable> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    T id;

    @Column(name = "created_by")
    @CreatedBy
    String createdBy;

    @Column(name = "updated_by")
    @LastModifiedBy
    String updateBy;

    @Column(name = "create_at")
    @CreationTimestamp
    LocalDateTime createdAt;

    @Column(name = "update_at")
    @UpdateTimestamp
    LocalDateTime updatedAt;

}
// @MappedSuperclass trong Spring (đặc biệt là trong JPA/Hibernate)
// được sử dụng để định nghĩa một lớp cha chứa các trường (fields)
// và mapping (liên kết) chung cho các lớp entity khác

