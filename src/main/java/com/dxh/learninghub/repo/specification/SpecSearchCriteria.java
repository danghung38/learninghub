package com.dxh.learninghub.repo.specification;

import lombok.Getter;

@Getter
public class SpecSearchCriteria {

    private final String          key;
    private final SearchOperation operation;
    private final Object          value;
    private final boolean         orPredicate;

    public SpecSearchCriteria(String orPredicate, String key, String operation,
                              String prefix, Object value, String suffix) {
        this.orPredicate = SearchOperation.OR_PREDICATE_FLAG.equals(orPredicate);
        this.key         = key;
        this.value       = value;
        this.operation   = SearchOperation.resolve(operation.charAt(0), prefix, suffix);
    }
}