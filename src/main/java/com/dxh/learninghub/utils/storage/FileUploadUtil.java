package com.dxh.learninghub.utils.storage;

import com.dxh.learninghub.exception.AppException;
import com.dxh.learninghub.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public final class FileUploadUtil {

    private FileUploadUtil() {}

    public static String validate(MultipartFile file, UploadPolicy policy) {
        if (file == null || file.isEmpty()) throw new AppException(ErrorCode.FILE_REQUIRED);
        return validate(file.getOriginalFilename(), file.getSize(), policy);
    }

    public static void validateIfPresent(MultipartFile file, UploadPolicy policy) {
        if (file != null && !file.isEmpty()) validate(file, policy);
    }

    public static String validate(String rawFileName, long fileSize, UploadPolicy policy) {
        String safeName = sanitizeFileName(rawFileName, policy);
        String ext = safeName.substring(safeName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);

        if (!policy.getAllowedExtensions().contains(ext)) throw new AppException(policy.getInvalidTypeError());
        if (fileSize <= 0 || fileSize > policy.getMaxSize()) throw new AppException(ErrorCode.FILE_TOO_LARGE);

        return safeName;
    }

    private static String sanitizeFileName(String fileName, UploadPolicy policy) {
        if (fileName == null || fileName.isBlank()) throw new AppException(policy.getInvalidTypeError());

        // Lấy tên file gốc (loại bỏ đường dẫn nếu có) và thay kí tự đặc biệt thành '_'
        String cleanName = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1)
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        int lastDot = cleanName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == cleanName.length() - 1) throw new AppException(policy.getInvalidTypeError());
        return cleanName;
    }
}