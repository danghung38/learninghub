package com.dxh.learninghub.repo.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class GenericSpecificationBuilder<T> {

    private final List<SpecSearchCriteria> criteria = new ArrayList<>();

    public GenericSpecificationBuilder<T> with(SpecSearchCriteria criterion) {
        criteria.add(criterion);
        return this;
    }

    public Specification<T> build() {
        List<GenericSpecification<T>> specs = criteria.stream()
                .map(c -> new GenericSpecification<T>(c))
                .toList();

        Specification<T> result = Specification.where(null);
        for (GenericSpecification<T> spec : specs) {
            result = spec.getCriteria().isOrPredicate()
                    ? result.or(spec)
                    : result.and(spec);
        }
        return result;
    }
}