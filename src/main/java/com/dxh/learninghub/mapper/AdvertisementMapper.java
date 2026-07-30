package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.request.AdvertisementCreationRequest;
import com.dxh.learninghub.dto.request.AdvertisementUpdateRequest;
import com.dxh.learninghub.dto.response.AdvertisementResponse;
import com.dxh.learninghub.entity.Advertisement;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface AdvertisementMapper {

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    Advertisement toEntity(AdvertisementCreationRequest request);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void update(AdvertisementUpdateRequest request, @MappingTarget Advertisement advertisement);

    @Mapping(source = "image", target = "image", qualifiedByName = "resolveFileUrl")
    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.title", target = "courseTitle")
    AdvertisementResponse toResponse(Advertisement advertisement);
}
