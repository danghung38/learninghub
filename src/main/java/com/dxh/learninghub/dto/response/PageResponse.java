package com.dxh.learninghub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
    int pageNo,

    int pageSize,

    int totalPage,

    long totalElements,

    List<T> items
) implements Serializable {}
