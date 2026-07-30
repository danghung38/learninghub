package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.request.LessonRequest;
import com.dxh.learninghub.dto.request.LessonUpdateRequest;
import com.dxh.learninghub.dto.response.LessonResponse;
import com.dxh.learninghub.entity.Lesson;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface LessonMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "contentUrl", ignore = true)
    Lesson lessonRequestToLesson(LessonRequest request);

    @Mapping(target = "chapterId", source = "chapter.id")
    @Mapping(
            target = "contentUrl",
            source = "contentUrl",
            qualifiedByName = "generateLessonViewUrl")
    LessonResponse lessonToLessonResponse(Lesson lesson);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(source = "contentUrl", target = "contentUrl", qualifiedByName = "normalizeObjectKey")
    void updateLessonFromRequest(LessonUpdateRequest request, @MappingTarget Lesson lesson);
}

