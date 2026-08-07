package com.dxh.learninghub.service.impl;

import com.dxh.learninghub.dto.request.TeacherRegisterRequest;
import com.dxh.learninghub.dto.request.TeacherUpdateRequest;
import com.dxh.learninghub.dto.response.TeacherResponse;
import com.dxh.learninghub.dto.response.PageResponse;
import com.dxh.learninghub.dto.response.TeacherCourseStudentResponse;
import com.dxh.learninghub.entity.Course;
import com.dxh.learninghub.entity.Enrollment;
import com.dxh.learninghub.entity.User;
import com.dxh.learninghub.enums.RegistrationStatus;
import com.dxh.learninghub.enums.RoleEnum;
import com.dxh.learninghub.enums.CourseStatus;
import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import com.dxh.learninghub.mapper.UserMapper;
import com.dxh.learninghub.repo.UserRepository;
import com.dxh.learninghub.repo.CourseRepository;
import com.dxh.learninghub.repo.EnrollmentRepository;
import com.dxh.learninghub.service.AwsS3Service;
import com.dxh.learninghub.service.interfac.TeacherService;
import com.dxh.learninghub.service.interfac.NotificationService;
import com.dxh.learninghub.utils.storage.FileUploadUtil;
import com.dxh.learninghub.utils.storage.UploadPolicy;
import com.dxh.learninghub.utils.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TeacherServiceImpl implements TeacherService {

    UserRepository userRepository;
    UserMapper userMapper;
    CurrentUserProvider currentUserProvider;
    AwsS3Service awsS3Service;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;
    NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public TeacherResponse getTeacherProfile() {

        User teacher = getCurrentTeacher();

        return userMapper.toTeacherResponse(teacher);
    }


    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public TeacherResponse registerTeacher(
            TeacherRegisterRequest request,
            MultipartFile cv,
            MultipartFile certificate) {

        User user = currentUserProvider.getCurrentUser();

        if (user.getRegistrationStatus() == RegistrationStatus.PENDING
                || user.getRegistrationStatus() == RegistrationStatus.APPROVED
                || user.getRegistrationStatus() == RegistrationStatus.REJECTED ){
            throw new AppException(ErrorCode.REGISTER_TEACHER_INVALID);
        }

        FileUploadUtil.validate(cv, UploadPolicy.TEACHER_DOCUMENT);
        FileUploadUtil.validate(certificate, UploadPolicy.TEACHER_DOCUMENT);
        user.setExpertise(request.expertise());
        user.setYearsOfExperience(request.yearsOfExperience());
        user.setBio(request.bio());
        user.setFacebookLink(request.facebookLink());

        // Đăng ký lần đầu -> chưa có file cũ, upload thẳng, không cần lo thứ tự xóa/tạo
        awsS3Service.uploadFile(cv, "teachers/" + user.getId() + "/cv", UploadPolicy.TEACHER_DOCUMENT);
        awsS3Service.uploadFile(certificate, "teachers/" + user.getId() + "/certificates", UploadPolicy.TEACHER_DOCUMENT);


        user.setRegistrationStatus(RegistrationStatus.PENDING);

        User savedUser = userRepository.save(user);

        userRepository.findFirstByRoles_Name(RoleEnum.ADMIN.name())
                .ifPresent(admin -> notificationService.createNotification(
                        admin,
                        savedUser,
                        "New teacher application",
                        savedUser.getFullName()
                                + " submitted a teacher application",
                        "/admin/teachers"
                ));

        return userMapper.toTeacherResponse(savedUser);
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public TeacherResponse reRegisterTeacher(
            TeacherRegisterRequest request,
            MultipartFile cv,
            MultipartFile certificate) {

        User user = currentUserProvider.getCurrentUser();
        if (user.getRegistrationStatus() != RegistrationStatus.PENDING
                && user.getRegistrationStatus() != RegistrationStatus.REJECTED) {
            throw new AppException(ErrorCode.TEACHER_REREGISTRATION_NOT_ALLOWED);
        }

        FileUploadUtil.validate(cv, UploadPolicy.TEACHER_DOCUMENT);
        FileUploadUtil.validate(certificate, UploadPolicy.TEACHER_DOCUMENT);

        String oldCv = user.getCvUrl();
        String oldCertificate = user.getCertificateUrl();
        String newCv = awsS3Service.uploadFile(cv, "teachers/" + user.getId() + "/cv", UploadPolicy.TEACHER_DOCUMENT);
        String newCertificate = awsS3Service.uploadFile(certificate, "teachers/" + user.getId() + "/certificates", UploadPolicy.TEACHER_DOCUMENT);




        user.setExpertise(request.expertise());
        user.setYearsOfExperience(request.yearsOfExperience());
        user.setBio(request.bio());
        user.setFacebookLink(request.facebookLink());
        user.setCvUrl(newCv);
        user.setCertificateUrl(newCertificate);
        user.setRegistrationStatus(RegistrationStatus.PENDING);

        User savedUser = userRepository.save(user);

        awsS3Service.deleteFileFromS3(oldCv);
        awsS3Service.deleteFileFromS3(oldCertificate);

        userRepository.findFirstByRoles_Name(RoleEnum.ADMIN.name())
                .ifPresent(admin -> notificationService.createNotification(
                        admin,
                        savedUser,
                        "Teacher application resubmitted",
                        savedUser.getFullName()
                                + " resubmitted a teacher application",
                        "/admin/teachers"
                ));

        return userMapper.toTeacherResponse(savedUser);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public TeacherResponse updateTeacherProfile(
            TeacherUpdateRequest request,
            MultipartFile cv,
            MultipartFile certificate) {

        User user = getCurrentTeacher();

        FileUploadUtil.validateIfPresent(cv, UploadPolicy.TEACHER_DOCUMENT);
        FileUploadUtil.validateIfPresent(certificate, UploadPolicy.TEACHER_DOCUMENT);
        userMapper.updateTeacherFromRequest(request, user);

        //replace file cũ
        boolean hasNewCv = cv != null && !cv.isEmpty();
        boolean hasNewCertificate = certificate != null && !certificate.isEmpty();
        String oldCv = user.getCvUrl();
        String oldCertificate = user.getCertificateUrl();
        String newCv = hasNewCv
                ? awsS3Service.uploadFile(cv, "teachers/" + user.getId() + "/cv", UploadPolicy.TEACHER_DOCUMENT)
                : null;
        String newCertificate = hasNewCertificate
                ? awsS3Service.uploadFile(certificate, "teachers/" + user.getId() + "/certificates", UploadPolicy.TEACHER_DOCUMENT)
                : null;

        if (hasNewCv) user.setCvUrl(newCv);
        if (hasNewCertificate) user.setCertificateUrl(newCertificate);

        User savedUser = userRepository.save(user);
        if (hasNewCv) awsS3Service.deleteFileFromS3(oldCv);
        if (hasNewCertificate) awsS3Service.deleteFileFromS3(oldCertificate);
        return userMapper.toTeacherResponse(savedUser);
    }

    private User getCurrentTeacher() {
        User user = currentUserProvider.getCurrentUser();
        boolean isTeacherOrAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleEnum.TEACHER.name())
                        || role.getName().equals(RoleEnum.ADMIN.name()));
        if (!isTeacherOrAdmin) throw new AppException(ErrorCode.USER_NOT_TEACHER);
        return user;
    }


    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    public PageResponse<TeacherCourseStudentResponse> getCourseStudents(
            Long courseId,
            Pageable pageable) {
        User currentUser = currentUserProvider.getCurrentUser();
        Course course = courseRepository.findWithAuthorById(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_EXISTED));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> RoleEnum.ADMIN.name().equals(role.getName()));
        boolean isCourseOwner = course.getAuthor() != null
                && Objects.equals(course.getAuthor().getId(), currentUser.getId());

        if (!isAdmin && !isCourseOwner) {
            throw new AppException(ErrorCode.NOT_COURSE_OWNER);
        }
        if (!isAdmin && course.getStatus() == CourseStatus.DELETED) {
            throw new AppException(ErrorCode.COURSE_NOT_AVAILABLE);
        }

        Page<Enrollment> students =
                enrollmentRepository.findByCourseIdOrderByCreatedAtDesc(courseId, pageable);

        return PageResponse.<TeacherCourseStudentResponse>builder()
                .pageNo(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalPage(students.getTotalPages())
                .totalElements(students.getTotalElements())
                .items(students.stream().map(this::toStudentResponse).toList())
                .build();
    }

    private TeacherCourseStudentResponse toStudentResponse(Enrollment enrollment) {
        return TeacherCourseStudentResponse.builder()
                .userId(enrollment.getUser().getId())
                .fullName(enrollment.getUser().getFullName())
                .avatar(awsS3Service.resolveFileUrl(enrollment.getUser().getAvatar()))
                .courseTitle(enrollment.getCourse().getTitle())
                .enrollmentStatus(enrollment.getStatus())
                .enrolledAt(enrollment.getCreatedAt())
                .build();
    }
}
