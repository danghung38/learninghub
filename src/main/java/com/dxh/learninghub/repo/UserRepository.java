package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    List<User> findByRoles_Name(String roleName);
    Optional<User> findByUsernameOrEmail(String username, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id in :userIds order by u.id")
    List<User> findAllByIdForUpdate(@Param("userIds") Collection<Long> userIds);

    Optional<User> findFirstByRoles_Name(String roleName);

    List<User> findAllByEnabledTrueAndBannedFalse();

    @Query("select user from User user where user.registrationStatus = com.dxh.learninghub.enums.RegistrationStatus.PENDING")
    Page<User> findPendingTeacherApplications(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(Long id);
}
