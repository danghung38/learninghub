package com.dxh.learninghub.mapper;

import com.dxh.learninghub.dto.response.CertificateResponse;
import com.dxh.learninghub.dto.response.CertificateVerificationResponse;
import com.dxh.learninghub.entity.Certificate;
import com.dxh.learninghub.service.AwsS3Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = AwsS3Service.class)
public interface CertificateMapper {

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.title", target = "courseName")
    @Mapping(source = "course.author.fullName", target = "instructor")
    @Mapping(source = "course.thumbnail", target = "thumbnail", qualifiedByName = "resolveFileUrl")
    CertificateResponse toResponse(Certificate certificate);

    @Mapping(source = "user.fullName", target = "recipient")
    @Mapping(source = "course.title", target = "courseName")
    @Mapping(source = "course.author.fullName", target = "instructor")
    @Mapping(target = "valid", constant = "true")
    CertificateVerificationResponse toVerificationResponse(Certificate certificate);
}
