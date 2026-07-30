package com.dxh.learninghub.mapper;


import com.dxh.learninghub.dto.request.ChapterRequest;
import com.dxh.learninghub.dto.request.ChapterUpdateRequest;
import com.dxh.learninghub.dto.response.ChapterResponse;
import com.dxh.learninghub.entity.Chapter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface ChapterMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Chapter toChapter(ChapterRequest request);

    @Mapping(source = "course.id", target = "courseId")
    ChapterResponse toChapterResponse(Chapter chapter);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateFromRequest(ChapterUpdateRequest request, @MappingTarget Chapter chapter);
}
