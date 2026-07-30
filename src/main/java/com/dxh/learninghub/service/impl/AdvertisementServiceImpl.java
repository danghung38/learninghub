package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.AdvertisementCreationRequest;
import com.dxh.learninghub.dto.request.AdvertisementUpdateRequest;
import com.dxh.learninghub.dto.response.AdvertisementResponse;
import com.dxh.learninghub.entity.Advertisement;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.AdvertisementMapper;
import com.dxh.learninghub.repo.AdvertisementRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.AdvertisementService;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdvertisementServiceImpl implements AdvertisementService {

    AdvertisementRepository advertisementRepository;
    CourseRepository courseRepository;
    AdvertisementMapper advertisementMapper;
    AwsS3Service awsS3Service;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse create(AdvertisementCreationRequest request, MultipartFile image) {
        FileUploadUtil.validate(image, UploadPolicy.IMAGE);
        validateDateRange(request.startDate(), request.endDate());

        Advertisement advertisement = advertisementMapper.toEntity(request);
        advertisement.setCourse(findCourse(request.courseId()));
        advertisement.setImage(awsS3Service.uploadAdvertisementImage(image));
        advertisement.setActive(true);
        return advertisementMapper.toResponse(advertisementRepository.save(advertisement));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse update(Long id, AdvertisementUpdateRequest request, MultipartFile image) {

        FileUploadUtil.validateIfPresent(image, UploadPolicy.IMAGE);
        Advertisement advertisement = findAdvertisement(id);
        String oldImage = advertisement.getImage();
        advertisementMapper.update(request, advertisement);
        if (request.courseId() != null) {
            advertisement.setCourse(findCourse(request.courseId()));
        }

        if (advertisement.getCourse() != null && advertisement.getCourse().getStatus() != CourseStatus.APPROVED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }
        validateDateRange(advertisement.getStartDate(), advertisement.getEndDate());

        boolean hasNewImage = image != null && !image.isEmpty();
        if (hasNewImage) {
            advertisement.setImage(awsS3Service.uploadAdvertisementImage(image));
        }

        Advertisement savedAdvertisement = advertisementRepository.save(advertisement);
        if (hasNewImage) {
            awsS3Service.deleteFileFromS3(oldImage);
        }
        return advertisementMapper.toResponse(savedAdvertisement);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse deactivate(Long id) {
        Advertisement advertisement = findAdvertisement(id);
        advertisement.setActive(false);
        return advertisementMapper.toResponse(advertisement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdvertisementResponse> getActiveAdvertisements() {
        return advertisementRepository
                .findActiveAdvertisements()
                .stream()
                .map(advertisementMapper::toResponse)
                .toList();
    }

    private Course findCourse(Long courseId) {
        if (courseId == null) return null;
        return courseRepository.findPublicCourseById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_AVAILABLE));
    }

    private Advertisement findAdvertisement(Long id) {
        return advertisementRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADVERTISEMENT_NOT_EXISTED));
    }


    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_ADVERTISEMENT_DATE_RANGE);
        }
    }
}
