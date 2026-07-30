package com.dxh.learninghub.repo;

import com.dxh.learninghub.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String roleName);
    boolean existsByName(String roleName);

    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findAll();
}
