package com.dxh.learninghub.mapper;


import com.dxh.learninghub.dto.request.CourseUploadRequest;
import com.dxh.learninghub.dto.request.CourseUpdateRequest;
import com.dxh.learninghub.dto.response.*;
import com.dxh.learninghub.entity.Chapter;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Lesson;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface CourseMapper {

    @Mapping(target = "author", source = "author.fullName")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(source = "thumbnail", target = "thumbnail", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "videoUrl", target = "videoUrl", qualifiedByName = "resolveFileUrl")
    CourseResponse courseToCourseResponse(Course course);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(source = "videoUrl", target = "videoUrl", qualifiedByName = "normalizeObjectKey")
    Course courseUploadToCourse(CourseUploadRequest course);

    @Mapping(target = "author", source = "author.fullName")
    @Mapping(source = "thumbnail", target = "thumbnail", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "videoUrl", target = "videoUrl", qualifiedByName = "resolveFileUrl")
    CourseUploadResponse courseToCourseUploadResponse(Course course);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(source = "videoUrl", target = "videoUrl", qualifiedByName = "normalizeObjectKey")
    void updateCourseFromRequest(
            CourseUpdateRequest request,
            @MappingTarget Course course);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.fullName", target = "authorName")
    @Mapping(source = "thumbnail", target = "thumbnail", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "videoUrl", target = "videoUrl", qualifiedByName = "resolveFileUrl")
    CoursePreviewResponse courseToCoursePreviewResponse(Course course);

    ChapterPreviewResponse chapterToChapterPreviewResponse(Chapter chapter);

    LessonPreviewResponse lessonToLessonPreviewResponse(Lesson lesson);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.fullName", target = "authorName")
    @Mapping(source = "thumbnail", target = "thumbnail", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "videoUrl", target = "videoUrl", qualifiedByName = "resolveFileUrl")
    CourseManagementPreviewResponse courseToManagementPreviewResponse(Course course);

    ChapterManagementPreviewResponse chapterToManagementPreviewResponse(Chapter chapter);

    @Mapping(source = "chapter.id", target = "chapterId")
    @Mapping(source = "contentUrl", target = "contentUrl", qualifiedByName = "generateLessonViewUrl")
    LessonResponse lessonToManagementPreviewResponse(Lesson lesson);
}
