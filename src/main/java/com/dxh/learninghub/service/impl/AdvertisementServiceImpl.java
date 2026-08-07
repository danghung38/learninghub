package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.AdvertisementCreationRequest;
import com.dxh.learninghub.dto.request.AdvertisementUpdateRequest;
import com.dxh.learninghub.dto.response.AdvertisementResponse;
import com.dxh.learninghub.entity.Advertisement;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.AdvertisementMapper;
import com.dxh.learninghub.repo.AdvertisementRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.EmailService;
import com.dxh.learninghub.service.interfac.AdvertisementService;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    UserRepository userRepository;
    NotificationService notificationService;
    EmailService emailService;

    @NonFinal
    @Value("${app.frontend-url:http://localhost:5173}")
    String frontendUrl;

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse create(AdvertisementCreationRequest request, MultipartFile image) {
        validateDateRange(request.startDate(), request.endDate());

        Advertisement advertisement = advertisementMapper.toEntity(request);
        advertisement.setCourse(findCourse(request.courseId()));
        advertisement.setImage(awsS3Service.uploadFile(image, "advertisements", UploadPolicy.IMAGE));
        advertisement.setLink(buildCourseLink(advertisement.getCourse()));
        advertisement.setActive(true);
        advertisement.setSent(false);
        return advertisementMapper.toResponse(advertisementRepository.save(advertisement));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse update(Long id, AdvertisementUpdateRequest request, MultipartFile image) {
        Advertisement advertisement = findAdvertisement(id);
        String oldImage = advertisement.getImage();
        advertisementMapper.update(request, advertisement);
        advertisement.setCourse(findCourse(request.courseId()));

        if (advertisement.getCourse() != null
                && advertisement.getCourse().getStatus() != CourseStatus.APPROVED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }
        validateDateRange(advertisement.getStartDate(), advertisement.getEndDate());

        boolean hasNewImage = image != null && !image.isEmpty();
        if (hasNewImage) {
            advertisement.setImage(awsS3Service.uploadFile(image, "advertisements", UploadPolicy.IMAGE));
        }

        advertisement.setLink(buildCourseLink(advertisement.getCourse()));
        advertisement.setSent(false);

        Advertisement savedAdvertisement = advertisementRepository.save(advertisement);

        if (hasNewImage && oldImage != null && !oldImage.isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    awsS3Service.deleteFileFromS3(oldImage);
                }
            });
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
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse activate(Long id) {
        Advertisement advertisement = findAdvertisement(id);
        advertisement.setActive(true);
        return advertisementMapper.toResponse(advertisement);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AdvertisementResponse> getAllAdvertisements(Boolean active, Boolean sent, String title) {
        String normalizedTitle = title == null || title.isBlank()
                ? null
                : title.trim();

        return advertisementRepository.searchAdvertisements(active, sent, normalizedTitle)
                .stream()
                .map(this::refreshGeneratedLink)
                .map(advertisementMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdvertisementResponse> getActiveAdvertisements() {
        return advertisementRepository
                .findActiveAdvertisements()
                .stream()
                .map(this::refreshGeneratedLink)
                .map(advertisementMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse sendNotification(Long id) {
        Advertisement advertisement = findAdvertisement(id);
        if (advertisement.isSent()) {
            throw new AppException(ErrorCode.ADVERTISEMENT_ALREADY_SENT);
        }

        String title = "New announcement: " + advertisement.getTitle();
        String message = advertisement.getDescription() == null
                || advertisement.getDescription().isBlank()
                ? "Discover the latest update from Learning Hub."
                : advertisement.getDescription().trim();
        String courseLink = buildFrontendUrl(advertisement.getLink());

        for (User user : userRepository.findAllByEnabledTrueAndBannedFalse()) {
            notificationService.createNotification(
                    user,
                    null,
                    title,
                    message,
                    courseLink);

//            emailService.sendAdvertisementEmail(
//                    user.getEmail(),
//                    user.getFullName() == null ? user.getUsername() : user.getFullName(),
//                    advertisement.getTitle(),
//                    message,
//                    courseLink);
        }

        // Kafka migration point: publish one advertisement event here later.
        advertisement.setSent(true);
        return advertisementMapper.toResponse(advertisement);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void sendTestNotification(Long advertisementId, String email) {
        Advertisement advertisement = findAdvertisement(advertisementId);
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String title = "New announcement: " + advertisement.getTitle();
        String message = advertisement.getDescription() == null
                || advertisement.getDescription().isBlank()
                ? "Discover the latest update from Learning Hub."
                : advertisement.getDescription().trim();
        String courseLink = buildFrontendUrl(advertisement.getLink());

        notificationService.createNotification(
                user,
                null,
                title,
                message,
                courseLink);

        emailService.sendAdvertisementEmail(
                user.getEmail(),
                user.getFullName() == null ? user.getUsername() : user.getFullName(),
                advertisement.getTitle(),
                message,
                courseLink);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdvertisementResponse resetSent(Long id) {
        Advertisement advertisement = findAdvertisement(id);
        advertisement.setSent(false);
        return advertisementMapper.toResponse(advertisement);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(Long id) {
        Advertisement advertisement = findAdvertisement(id);
        advertisementRepository.delete(advertisement);
        awsS3Service.deleteFileFromS3(advertisement.getImage());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void deleteExpiredAdvertisements() {
        LocalDate today = LocalDate.now();
        List<Advertisement> expiredAds = advertisementRepository.findExpiredAdvertisements(today);

        if (expiredAds.isEmpty()) {
            return;
        }

        // Xóa hình ảnh trên S3 trước khi xóa record trong DB
        for (Advertisement ad : expiredAds) {
            if (ad.getImage() != null && !ad.getImage().isBlank()) {
                awsS3Service.deleteFileFromS3(ad.getImage());
            }
        }

        // Xóa tất cả quảng cáo hết hạn trong DB
        advertisementRepository.deleteAll(expiredAds);
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findPublicCourseById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_AVAILABLE));
    }

    private String buildCourseLink(Course course) {
        return "/courses/" + course.getId();
    }

    private String buildFrontendUrl(String path) {
        String baseUrl = frontendUrl == null ? "" : frontendUrl.strip();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private Advertisement findAdvertisement(Long id) {
        Advertisement advertisement = advertisementRepository.findWithCourseById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ADVERTISEMENT_NOT_EXISTED));
        return refreshGeneratedLink(advertisement);
    }

    private Advertisement refreshGeneratedLink(Advertisement advertisement) {
        if (advertisement.getCourse() != null) {
            advertisement.setLink(buildCourseLink(advertisement.getCourse()));
        }
        return advertisement;
    }


    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_ADVERTISEMENT_DATE_RANGE);
        }
    }
}
