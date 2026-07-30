package com.dxh.learninghub.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    /*
     * Error Code Convention
     *
     * 1000 - 1099 : Common / Validation
     * 1100 - 1199 : Authentication / Authorization
     * 1200 - 1299 : User
     * 1300 - 1399 : Course
     * 1400 - 1499 : Chapter
     * 1500 - 1599 : Lesson
     * 1600 - 1699 : Favorite
     * 1700 - 1799 : Learning Progress
     * 1800 - 1899 : File / Upload
     * 1900 - 1999 : Teacher / Admin
     * 2000 - 2099 : Conversation
     * 2100 - 2199 : Review
     */

    UNCATEGORIZED_EXCEPTION(1000, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    MIN_INVALID(1002, "Value must be at least {value}", HttpStatus.BAD_REQUEST),
    MAX_INVALID(1003, "Value must be at most {value}", HttpStatus.BAD_REQUEST),
    INVALID_DATA_TYPE(1004, "Invalid data type.", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1005, "Invalid request", HttpStatus.BAD_REQUEST),
    INVALID_NULL(1006, "Please do not leave the required information blank", HttpStatus.BAD_REQUEST),
    INVALID_BLANK(1007, "Please do not leave the required information blank", HttpStatus.BAD_REQUEST),
    INVALID_EMPTY(1008, "Please do not leave the required information empty", HttpStatus.BAD_REQUEST),
    CONTENT_TOO_LONG(1009, "The content must not exceed {max} characters", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_NUMBER(1010, "Page number must be at least 1", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_SIZE(1011, "Page size must be between 1 and 100", HttpStatus.BAD_REQUEST),
    INVALID_SORT_FORMAT(1012, "Sort must use the format field:asc or field:desc", HttpStatus.BAD_REQUEST),
    INVALID_SORT_FIELD(1013, "Sort field is not allowed", HttpStatus.BAD_REQUEST),
    INVALID_SORT_DIRECTION(1014, "Sort direction must be asc or desc", HttpStatus.BAD_REQUEST),
    INVALID_FILE_SIZE(1015, "File size must be greater than 0", HttpStatus.BAD_REQUEST),
    INVALID_URL(1016, "Invalid URL", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1100, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1101, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_TOKEN_TYPE(1102, "Invalid token type", HttpStatus.UNAUTHORIZED),
    INVALID_GOOGLE_TOKEN(1103, "Invalid Google token", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(1104, "Invalid username or password", HttpStatus.BAD_REQUEST),
    ACCOUNT_BANNED(1105, "Account has been banned", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_VERIFIED(1106, "Account not verified. Please check your email to verify your account.", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS(1107, "Too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),

    USER_EXISTED(1200, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1201, "User not existed", HttpStatus.NOT_FOUND),
    PHONE_EXISTED(1202, "Phone existed", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1203, "email existed", HttpStatus.BAD_REQUEST),
    INVALID_VERIFY_KEY(1204, "verify key not match", HttpStatus.BAD_REQUEST),
    VERIFY_KEY_EXPIRED(1205, "verify key expired", HttpStatus.BAD_REQUEST),
    ALREADY_VERIFIED(1206, "user already verified", HttpStatus.BAD_REQUEST),
    SEND_FAILED(1207, "Send email verify failed", HttpStatus.BAD_REQUEST),
    USER_ALREADY_BANNED(1208, "User is already banned", HttpStatus.BAD_REQUEST),
    USER_NOT_BANNED(1209, "User is not banned", HttpStatus.BAD_REQUEST),
    USER_NOT_VERIFIED(1210, "User is not verified", HttpStatus.BAD_REQUEST),
    USER_NOT_TEACHER(1211, "User is not a teacher", HttpStatus.FORBIDDEN),
    USERNAME_INVALID(1212, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_FORMAT(1213, "Username may only contain letters, numbers, and underscores", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1214, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_NAME(1215, "Name cannot be left blank", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER(1216, "Enter correct phone number format", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1217, "Enter correct email format", HttpStatus.BAD_REQUEST),
    INVALID_GENDER(1218, "gender must be any of {MALE, FEMALE, OTHERS}", HttpStatus.BAD_REQUEST),
    INVALID_DOB(1219, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT_DOB(1220, "Dob must be format yyyy/MM/dd", HttpStatus.BAD_REQUEST),
    USER_ALREADY_HAS_ROLE(1221, "User already has the specified role", HttpStatus.CONFLICT),
    USER_DOES_NOT_HAVE_ROLE(1222, "User does not have the specified role", HttpStatus.BAD_REQUEST),
    USER_MUST_HAVE_AT_LEAST_ONE_ROLE(1223, "User must have at least one role", HttpStatus.BAD_REQUEST),
    PASSWORD_ALREADY_CREATED(1224, "Password has already been created for this account", HttpStatus.CONFLICT),
    PASSWORD_CONFIRMATION_MISMATCH(1225, "Password confirmation does not match", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_CREATED(1226, "Password has not been created for this account", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INCORRECT(1227, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_MUST_BE_DIFFERENT(1228, "New password must be different from the current password", HttpStatus.BAD_REQUEST),

    COURSE_NOT_EXISTED(1300, "Course not existed", HttpStatus.NOT_FOUND),
    COURSE_NOT_AVAILABLE(1301, "Course is unavailable", HttpStatus.NOT_FOUND),
    COURSE_ALREADY_PURCHASED(1302, "You have already purchased this course", HttpStatus.BAD_REQUEST),
    BUY_COURSE_INVALID(1303, "Your points are insufficient to purchase this course", HttpStatus.BAD_REQUEST),
    CANNOT_BUY_OWN_COURSE(1304, "You cannot purchase your own course", HttpStatus.BAD_REQUEST),
    NOT_COURSE_OWNER(1305, "You are not the owner of this course", HttpStatus.FORBIDDEN),
    COURSE_ALREADY_DELEDED(1306, "Course is already soft-deleted", HttpStatus.BAD_REQUEST),
    COURSE_DELEDED_READ_ONLY(1307, "Soft-deleted courses cannot be modified", HttpStatus.CONFLICT),
    COURSE_NOT_BANNED(1308, "Course is not banned", HttpStatus.BAD_REQUEST),
    COURSE_BANNED_CANNOT_DELETE(
            1309,
            "A banned course cannot be soft-deleted by its owner",
            HttpStatus.CONFLICT),
    COURSE_NOT_DRAFT(
            1310,
            "Only a draft course can be submitted for approval",
            HttpStatus.CONFLICT),
    COURSE_NOT_PENDING(
            1311,
            "Only a pending course can be approved or rejected",
            HttpStatus.CONFLICT),
    COURSE_CONTENT_INCOMPLETE(
            1312,
            "A course must contain at least one chapter and one lesson before submission",
            HttpStatus.BAD_REQUEST),

    CHAPTER_NOT_EXISTED(1400, "Chapter not existed", HttpStatus.NOT_FOUND),
    NOT_CHAPTER_OWNER(1401, "You are not the owner of this chapter", HttpStatus.FORBIDDEN),

    LESSON_NOT_EXISTED(1500, "Lesson not existed", HttpStatus.NOT_FOUND),
    LESSON_ACCESS_DENIED(1501, "You do not have access to this lesson", HttpStatus.FORBIDDEN),
    LESSON_CROSS_COURSE_MOVE_NOT_ALLOWED(
            1502,
            "A lesson can only be moved between chapters in the same course",
            HttpStatus.BAD_REQUEST),

    FAVORITE_NOT_EXISTED(1600, "Favorite not existed", HttpStatus.NOT_FOUND),
    ALREADY_IN_FAVORITES(1601, "Course already in favorites", HttpStatus.BAD_REQUEST),

    COURSE_PROGRESS_NOT_EXISTED(1700, "Course progress not existed", HttpStatus.NOT_FOUND),

    UPLOAD_FAIL(1800, "Error uploading file to S3", HttpStatus.BAD_REQUEST),
    FILE_REQUIRED(1801, "File is required", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1802, "File size exceeds allowed limit", HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_FILE_TYPE(1803, "Only PDF, DOC and DOCX files are allowed", HttpStatus.BAD_REQUEST),
    NOT_FOUND_IMAGE(1804, "Image not found", HttpStatus.BAD_REQUEST),
    INVALID_FB_URL(1805, "Invalid Facebook URL", HttpStatus.BAD_REQUEST),
    INVALID_S3_URL(1806, "The S3 URL does not belong to the system bucket", HttpStatus.BAD_REQUEST),
    INVALID_AVATAR_FILE_TYPE(1808, "Avatar must be a JPG, PNG or WebP image", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_FILE_TYPE(1809, "Image must be a JPG, PNG or WebP file", HttpStatus.BAD_REQUEST),

    REGISTER_TEACHER_INVALID(1900, "You have already submitted a teacher registration request", HttpStatus.BAD_REQUEST),
    REGISTRATION_NOT_PENDING(1901, "Registration status is not pending", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXISTED(1902, "RoleEnum not existed", HttpStatus.NOT_FOUND),
    TEACHER_REREGISTRATION_NOT_ALLOWED(1903, "Only a pending or rejected teacher application can be updated", HttpStatus.BAD_REQUEST),
    ADMIN_NOT_FOUND(1905, "Admin not found", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REVENUE_REQUEST(1906, "Invalid revenue request", HttpStatus.BAD_REQUEST),

    CONVERSATION_NOT_FOUND_OR_FORBIDDEN(2000, "Conversation not found or access denied", HttpStatus.FORBIDDEN),
    CANNOT_CHAT_WITH_YOURSELF(2001, "You cannot start a conversation with yourself", HttpStatus.BAD_REQUEST),
    COURSE_CHAT_REQUIRES_ENROLLMENT(2002, "Only enrolled students can contact the course teacher", HttpStatus.FORBIDDEN),

    REVIEW_NOT_EXISTED(2100, "Review not existed", HttpStatus.NOT_FOUND),
    REVIEW_REQUIRES_ENROLLMENT(2101, "Only enrolled students can review this course", HttpStatus.FORBIDDEN),
    REVIEW_ALREADY_EXISTS(2102, "You have already reviewed this course", HttpStatus.CONFLICT),
    NOT_REVIEW_OWNER(2103, "You are not the owner of this review", HttpStatus.FORBIDDEN),
    CANNOT_REPLY_TO_REPLY(2106, "Nested review replies are not allowed", HttpStatus.BAD_REQUEST),
    REVIEW_REPLY_FORBIDDEN(2107, "Only the course owner or an admin can reply to reviews", HttpStatus.FORBIDDEN),
    CANNOT_UPDATE_REPLY_AS_REVIEW(2108, "A reply cannot be updated with the review endpoint", HttpStatus.BAD_REQUEST),

    INSUFFICIENT_USER_POINTS(2200, "User does not have enough points", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(2201, "From date must not be after to date", HttpStatus.BAD_REQUEST),
    POINT_TRANSACTION_NOT_EXISTED(2202, "Point transaction not existed", HttpStatus.NOT_FOUND),

    NOTIFICATION_NOT_EXISTED(2300, "Notification not existed", HttpStatus.NOT_FOUND),

    BANK_ACCOUNT_NOT_EXISTED(2400, "Bank account not existed", HttpStatus.NOT_FOUND),
    BANK_ACCOUNT_INACTIVE(2401, "Bank account is inactive", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_NOT_EXISTED(2402, "Withdrawal not existed", HttpStatus.NOT_FOUND),
    WITHDRAWAL_INSUFFICIENT_POINTS(2403, "Insufficient points for withdrawal", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_NOT_PENDING(2404, "Withdrawal is not pending", HttpStatus.CONFLICT),
    INVALID_PAYMENT_PROOF(2405, "Payment proof must be an image", HttpStatus.BAD_REQUEST),

    INVALID_DEPOSIT_AMOUNT(2500, "Deposit amount must be a positive multiple of 1,000 VND", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_EXISTED(2501, "Payment not existed", HttpStatus.NOT_FOUND),

    CERTIFICATE_NOT_EXISTED(2600, "Certificate not existed", HttpStatus.NOT_FOUND),
    COURSE_NOT_COMPLETED(2601, "Complete the course before requesting a certificate", HttpStatus.BAD_REQUEST),
    CERTIFICATE_GENERATION_FAILED(2602, "Failed to generate certificate", HttpStatus.INTERNAL_SERVER_ERROR),

    ADVERTISEMENT_NOT_EXISTED(2700, "Advertisement not existed", HttpStatus.NOT_FOUND),
    INVALID_ADVERTISEMENT_DATE_RANGE(2701, "Start date must not be after end date", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
