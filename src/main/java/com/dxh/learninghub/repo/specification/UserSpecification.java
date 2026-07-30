package com.dxh.learninghub.repo.specification;


import com.dxh.learninghub.dto.request.UserSearchFilterRequest;
import com.dxh.learninghub.entity.Role;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RoleEnum;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> getUsersWithFilter(UserSearchFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Loại bỏ bản ghi trùng lặp khi JOIN với danh sách roles
            query.distinct(true);

            // Tránh NullPointerException nếu lỡ filter bị null
            if (filter == null) return null;

            // 2. Lọc theo username
            if (StringUtils.hasText(filter.username())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")),
                        "%" + filter.username().toLowerCase().trim() + "%"
                ));
            }

            // 3. Lọc theo fullName
            if (StringUtils.hasText(filter.fullName())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")),
                        "%" + filter.fullName().toLowerCase().trim() + "%"
                ));
            }

            // 4. Lọc theo Role Name
            if (StringUtils.hasText(filter.role())) {
                Join<User, Role> roleJoin = root.join("roles", JoinType.LEFT);
                // Truyền trực tiếp chuỗi String đã chuẩn hóa (vì entity name là String)
                predicates.add(criteriaBuilder.equal(roleJoin.get("name"), filter.role().trim().toUpperCase()));
            }

            // 5. Lọc theo banned
            if (filter.banned() != null) {
                predicates.add(criteriaBuilder.equal(root.get("banned"), filter.banned()));
            }

            // 6. Lọc theo enabled
            if (filter.enabled() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), filter.enabled()));
            }

            return predicates.isEmpty() ? null : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}