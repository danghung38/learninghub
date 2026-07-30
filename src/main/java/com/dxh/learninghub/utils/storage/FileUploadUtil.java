package com.dxh.learninghub.utils.storage;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public final class FileUploadUtil {

    private FileUploadUtil() {
    }

    public static String validate(MultipartFile file, UploadPolicy policy) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_REQUIRED);
        }
        return validate(file.getOriginalFilename(), file.getSize(), policy);
    }

    public static void validateIfPresent(MultipartFile file, UploadPolicy policy) {
        if (file != null && !file.isEmpty()) {
            validate(file, policy);
        }
    }

    public static String validate(String originalFileName, long fileSize, UploadPolicy policy) {
        String safeFileName = sanitizeFileName(originalFileName, policy);
        String extension = safeFileName.substring(safeFileName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);

        if (!policy.allowedExtensions().contains(extension)) {
            throw new AppException(policy.invalidTypeError());
        }
        if (fileSize <= 0 || fileSize > policy.maxSize()) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        return safeFileName;
    }

    private static String sanitizeFileName(String fileName, UploadPolicy policy) {
        if (fileName == null || fileName.isBlank()) {
            throw new AppException(policy.invalidTypeError());
        }

        String normalized = fileName.trim().replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        if (name.isBlank() || !name.contains(".") || name.endsWith(".")) {
            throw new AppException(policy.invalidTypeError());
        }
        return name;
    }
}
