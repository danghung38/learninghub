package com.dxh.learninghub.repo.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PredicateBuilder {

    private PredicateBuilder() {}

    @SuppressWarnings("unchecked")
    public static Predicate build(Path<?> root, CriteriaBuilder cb, SpecSearchCriteria criteria) {

        String key = criteria.getKey();
        Object value = criteria.getValue();

        return switch (criteria.getOperation()) {

            case EQUALITY    -> cb.equal(root.get(key), convertValue(root.get(key), value));

            case NEGATION    -> cb.notEqual(root.get(key), convertValue(root.get(key), value));

            case GREATER_THAN -> cb.greaterThan(root.<Comparable>get(key),
                    (Comparable) convertValue(root.get(key), value));

            case LESS_THAN   -> cb.lessThan(root.<Comparable>get(key),
                    (Comparable) convertValue(root.get(key), value));

            case STARTS_WITH -> cb.like(root.get(key).as(String.class), value + "%");

            case ENDS_WITH   -> cb.like(root.get(key).as(String.class), "%" + value);

            case CONTAINS    -> cb.like(root.get(key).as(String.class), "%" + value + "%");
        };
    }

    private static Object convertValue(Path<?> path, Object value) {

        Class<?> type = path.getJavaType();
        String str = value.toString();

        if (type == LocalDate.class) {
            return LocalDate.parse(str);                    // "1990-01-15"
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.parse(str);                // "1990-01-15T00:00:00"
        }
        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(str);
        }
        if (type == Long.class || type == long.class) {
            return Long.parseLong(str);
        }
        if (type == Double.class || type == double.class) {
            return Double.parseDouble(str);                 // yearsOfExperience
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(str);               // enabled, banned
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, str);   // gender: MALE, FEMALE
        }

        return value; // String fallback
    }
}