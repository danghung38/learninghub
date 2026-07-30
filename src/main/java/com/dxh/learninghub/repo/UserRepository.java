package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
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

    @Query(value = """
            select distinct user
            from User user
            left join user.roles role
            where (:username is null
                    or lower(user.username) like lower(concat('%', :username, '%')))
              and (:fullName is null
                    or lower(user.fullName) like lower(concat('%', :fullName, '%')))
              and (:role is null or role.name = :role)
              and (:banned is null or user.banned = :banned)
              and (:enabled is null or user.enabled = :enabled)
            """,
            countQuery = """
            select count(distinct user.id)
            from User user
            left join user.roles role
            where (:username is null
                    or lower(user.username) like lower(concat('%', :username, '%')))
              and (:fullName is null
                    or lower(user.fullName) like lower(concat('%', :fullName, '%')))
              and (:role is null or role.name = :role)
              and (:banned is null or user.banned = :banned)
              and (:enabled is null or user.enabled = :enabled)
            """)
    Page<User> searchUsers(
            @Param("username") String username,
            @Param("fullName") String fullName,
            @Param("role") String role,
            @Param("banned") Boolean banned,
            @Param("enabled") Boolean enabled,
            Pageable pageable);

    @Query("select user from User user where user.registrationStatus = com.dxh.learninghub.enums.RegistrationStatus.PENDING")
    Page<User> findPendingTeacherApplications(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    List<User> findAllByIdIn(Collection<Long> userIds);
}
