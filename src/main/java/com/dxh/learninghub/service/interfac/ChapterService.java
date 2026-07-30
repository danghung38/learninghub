package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.ChapterRequest;
import com.dxh.learninghub.dto.request.ChapterUpdateRequest;
import com.dxh.learninghub.dto.response.ChapterResponse;

public interface ChapterService {
    ChapterResponse create(ChapterRequest request);
    ChapterResponse update(Long id, ChapterUpdateRequest request);
    void delete(Long id);
}