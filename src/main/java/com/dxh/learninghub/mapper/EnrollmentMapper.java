package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.response.BuyCourseResponse;
import com.dxh.learninghub.dto.response.MyCourseResponse;
import com.dxh.learninghub.entity.Enrollment;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface EnrollmentMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "title", source = "course.title")
    @Mapping(target = "author", source = "course.author.fullName")
    @Mapping(target = "courseLevel", source = "course.courseLevel")
    @Mapping(
            target = "thumbnail",
            source = "course.thumbnail",
            qualifiedByName = "resolveFileUrl")
    @Mapping(target = "points", source = "course.points")
    BuyCourseResponse toBuyCourseResponse(Enrollment enrollment);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "title", source = "course.title")
    @Mapping(target = "author", source = "course.author.fullName")
    @Mapping(target = "courseLevel", source = "course.courseLevel")
    @Mapping(
            target = "thumbnail",
            source = "course.thumbnail",
            qualifiedByName = "resolveFileUrl")
    @Mapping(target = "points", source = "course.points")
    @Mapping(target = "enrollmentStatus", source = "status")
    @Mapping(target = "enrolledAt", source = "createdAt")
    MyCourseResponse toMyCourseResponse(Enrollment enrollment);
}
