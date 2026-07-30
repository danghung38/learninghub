package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.response.NotificationResponse;
import com.dxh.learninghub.entity.Notification;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface NotificationMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(
            source = "avatarUrl",
            target = "avatarUrl",
            qualifiedByName = "resolveFileUrl")
    NotificationResponse toResponse(Notification notification);
}
