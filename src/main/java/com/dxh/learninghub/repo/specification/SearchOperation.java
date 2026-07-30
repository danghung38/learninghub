package com.dxh.learninghub.repo.specification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum SearchOperation {

    EQUALITY(':'),
    NEGATION('!'),
    GREATER_THAN('>'),
    LESS_THAN('<'),
    STARTS_WITH('^'),
    ENDS_WITH('$'),
    CONTAINS('~');

    public static final String OR_PREDICATE_FLAG = "'";
    private final char symbol;

    public static SearchOperation from(char symbol) {
        for (SearchOperation op : values()) {
            if (op.symbol == symbol) return op;
        }
        return null;
    }

    public static SearchOperation resolve(char symbol, String prefix, String suffix) {
        SearchOperation op = from(symbol);
        if (op != EQUALITY) return op;

        boolean startsWith = prefix != null && prefix.contains("*");
        boolean endsWith   = suffix  != null && suffix.contains("*");

        if (startsWith && endsWith) return CONTAINS;
        if (startsWith)             return ENDS_WITH;
        if (endsWith)               return STARTS_WITH;
        return EQUALITY;
    }
}