package com.dxh.learninghub.utils;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dxh.learninghub.constant.AppConstant.SORT_BY;

public class PageUtil {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> DEFAULT_SORT_FIELDS = Set.of(
            "id", "createdAt", "updatedAt");

    private PageUtil() {
    }

    public static Pageable createPageable(
            Integer pageNo,
            Integer pageSize,
            String sortBy,
            String... allowedSortFields) {
        if (pageNo == null || pageNo < 1) {
            throw new AppException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        if (pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_SIZE);
        }

        Set<String> allowedFields = allowedSortFields == null
                || allowedSortFields.length == 0
                ? DEFAULT_SORT_FIELDS
                : Arrays.stream(allowedSortFields).collect(Collectors.toUnmodifiableSet());

        int page = pageNo - 1;
        List<Sort.Order> sorts = new ArrayList<>();
        if (StringUtils.hasLength(sortBy)) {
            Pattern pattern = Pattern.compile(SORT_BY);
            Matcher matcher = pattern.matcher(sortBy);
            if (!matcher.matches()) {
                throw new AppException(ErrorCode.INVALID_SORT_FORMAT);
            }

            String field = matcher.group(1);
            String direction = matcher.group(3);
            if (!allowedFields.contains(field)) {
                throw new AppException(ErrorCode.INVALID_SORT_FIELD);
            }
            if (!direction.equalsIgnoreCase("asc")
                    && !direction.equalsIgnoreCase("desc")) {
                throw new AppException(ErrorCode.INVALID_SORT_DIRECTION);
            }
            sorts.add(new Sort.Order(Sort.Direction.fromString(direction), field));
        }

        return PageRequest.of(page, pageSize, Sort.by(sorts));
    }
}
