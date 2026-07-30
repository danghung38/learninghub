package com.dxh.learninghub.repo.specification;


import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> getUsersWithFilter(
            String username,
            String fullName,
            String role,
            Boolean banned,
            Boolean enabled) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Loại bỏ bản ghi trùng lặp khi JOIN với danh sách roles
            query.distinct(true);

            // 2. Lọc theo username (chỉ thêm khi username có giá trị)
            if (StringUtils.hasText(username)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")),
                        "%" + username.toLowerCase().trim() + "%"
                ));
            }

            // 3. Lọc theo fullName
            if (StringUtils.hasText(fullName)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")),
                        "%" + fullName.toLowerCase().trim() + "%"
                ));
            }

            // 4. Lọc theo Role Name (LEFT JOIN u.roles r)
            if (StringUtils.hasText(role)) {
                Join<User, Role> roleJoin = root.join("roles", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(roleJoin.get("name"), role));
            }

            // 5. Lọc theo banned
            if (banned != null) {
                predicates.add(criteriaBuilder.equal(root.get("banned"), banned));
            }

            // 6. Lọc theo enabled
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            // Kết hợp tất cả điều kiện bằng phép AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}