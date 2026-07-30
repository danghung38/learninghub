package com.dxh.learninghub.entity;

import com.dxh.learninghub.enums.Gender;
import com.dxh.learninghub.enums.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.BatchSize;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends AbstractEntity<Long> {

    @Column(unique = true, nullable = false)
    String username;

    @Enumerated(EnumType.STRING)
    Gender gender;

    String fullName;

    @Column(unique = true, nullable = false)
    String email;

    String avatar;

    @Column(unique = true)
    String phoneNumber;

    @Column(nullable = true)
    String password;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    Boolean enabled = false;

    @Builder.Default
    @Column(name = "banned", nullable = false)
    Boolean banned = false;

    @DateTimeFormat(pattern = "yyyy/MM/dd")
    LocalDate dob;

    @BatchSize(size = 20)//N+1
    @ManyToMany
    @Builder.Default
    Set<Role> roles = new LinkedHashSet<>();

    @Column(name = "address")
    String address;

    @Column(name = "description", columnDefinition = "MEDIUMTEXT")
    String description;

    @Column(name = "expertise")
    String expertise;

    @Column(name = "yearsOfExperience")
    Double yearsOfExperience;

    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;

    @Column(name = "certificate")
    String certificateUrl;

    @Column(name = "cvUrl")
    String cvUrl;

    @Column(unique = true)
    String googleId;

    @Column(name = "facebookLink")
    String facebookLink;

    @Builder.Default
    @Column(name = "points", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    Long points = 0L;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    Set<Course> courses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Review> reviews = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Enrollment> enrollments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Favorite> favorites = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    Set<PointTransaction> pointTransactions = new LinkedHashSet<>();

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    List<BankAccount> bankAccounts = new ArrayList<>();

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    List<Withdrawal> withdrawals = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false)
    RegistrationStatus registrationStatus = RegistrationStatus.NONE;

    @PrePersist
    protected void onCreate() {

        if (enabled == null) {
            enabled = false;
        }

        if (points == null) {
            points = 0L;
        }

        if(banned == null){
            banned = false;
        }

        if (registrationStatus == null) {
            registrationStatus = RegistrationStatus.NONE;
        }
    }

}
