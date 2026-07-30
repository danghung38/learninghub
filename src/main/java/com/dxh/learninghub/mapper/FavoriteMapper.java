package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.response.FavoriteResponse;
import com.dxh.learninghub.entity.Favorite;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface FavoriteMapper {

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.title", target = "courseTitle")
    @Mapping(
            source = "course.thumbnail",
            target = "thumbnail",
            qualifiedByName = "resolveFileUrl")
    @Mapping(source = "course.author.fullName", target = "authorName")
    @Mapping(source = "course.points", target = "points")
    @Mapping(source = "course.courseLevel", target = "courseLevel")
    @Mapping(source = "course.status", target = "status")
    FavoriteResponse toFavoriteResponse(Favorite favorite);
}
