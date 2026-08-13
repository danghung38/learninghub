package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.request.UserCreationRequest;
import com.dxh.learninghub.dto.request.UserUpdateRequest;
import com.dxh.learninghub.dto.request.TeacherUpdateRequest;
import com.dxh.learninghub.dto.response.TeacherResponse;
import com.dxh.learninghub.dto.response.TeacherCoursePreview;
import com.dxh.learninghub.dto.response.UserResponse;
import com.dxh.learninghub.dto.response.UserUpdateResponse;
import com.dxh.learninghub.dto.response.admin.TeacherApplicationDetailResponse;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {RoleMapper.class, AwsS3Service.class})
public interface UserMapper {
    @Mapping(source = "avatar", target = "avatar", qualifiedByName = "resolveFileUrl")
    TeacherCoursePreview userToTeacherCoursePreview(User user);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "password", ignore = true)
    User toUser(UserCreationRequest request);

    @Mapping(source = "avatar", target = "avatar", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "cvUrl", target = "cvUrl", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "certificateUrl", target = "certificateUrl", qualifiedByName = "resolveFileUrl")
    UserResponse toUserResponse(User user);

    // Thêm annotation này: Field nào null trong DTO sẽ tự động BỎ QUA, không map sang Entity
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateTeacherFromRequest(TeacherUpdateRequest request, @MappingTarget User user);

    @Mapping(source = "avatar", target = "avatar", qualifiedByName = "resolveFileUrl")
    UserUpdateResponse toUserUpdateResponse(User user);

    @Mapping(source = "avatar", target = "avatar", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "cvUrl", target = "cvUrl", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "certificateUrl", target = "certificate", qualifiedByName = "resolveFileUrl")
    TeacherApplicationDetailResponse toTeacherApplicationDetailResponse(User user);

    @Mapping(source = "cvUrl", target = "cvUrl", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "certificateUrl", target = "certificateUrl", qualifiedByName = "resolveFileUrl")
    TeacherResponse toTeacherResponse(User user);
}
