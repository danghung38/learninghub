package com.dxh.learninghub.utils.storage;

import com.dxh.learninghub.exception.ErrorCode;

import java.util.Set;

public enum UploadPolicy {
    VIDEO(500L * 1024 * 1024, Set.of("mp4", "webm"), ErrorCode.INVALID_FILE_TYPE),
    DOCUMENT(10L * 1024 * 1024, Set.of("pdf", "doc", "docx"), ErrorCode.INVALID_FILE_TYPE),
    TEACHER_DOCUMENT(5L * 1024 * 1024, Set.of("pdf", "doc", "docx"), ErrorCode.INVALID_FILE_TYPE),
    IMAGE(5L * 1024 * 1024, Set.of("jpg", "jpeg", "png", "webp"), ErrorCode.INVALID_IMAGE_FILE_TYPE),
    AVATAR(5L * 1024 * 1024, Set.of("jpg", "jpeg", "png", "webp"), ErrorCode.INVALID_AVATAR_FILE_TYPE),
    PAYMENT_PROOF(5L * 1024 * 1024, Set.of("jpg", "jpeg", "png", "webp"), ErrorCode.INVALID_PAYMENT_PROOF);

    private final long maxSize;
    private final Set<String> allowedExtensions;
    private final ErrorCode invalidTypeError;

    UploadPolicy(long maxSize, Set<String> allowedExtensions, ErrorCode invalidTypeError) {
        this.maxSize = maxSize;
        this.allowedExtensions = allowedExtensions;
        this.invalidTypeError = invalidTypeError;
    }

    long maxSize() {
        return maxSize;
    }

    Set<String> allowedExtensions() {
        return allowedExtensions;
    }

    ErrorCode invalidTypeError() {
        return invalidTypeError;
    }
}
