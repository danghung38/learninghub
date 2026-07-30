package com.dxh.learninghub.exception;


import com.dxh.learninghub.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    //các lỗi khác
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<String>> handlingRuntimeException(RuntimeException exception){
        log.error("Exception: ", exception);
        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .code(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .result(now)
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiResponse);
    }


    // Bắt lỗi Validation và Missing Multipart Part
    // Bắt lỗi Multipart & Thiếu RequestPart (không tranh chấp với handlingValidation)
    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MultipartException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleMultipartErrors(Exception ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        // 1. Xử lý thiếu Part (VD: thiếu file certificate, thiếu part teacher)
        if (ex instanceof MissingServletRequestPartException missing) {
            errors.put(missing.getRequestPartName(), "Missing required part: " + missing.getRequestPartName());
        }
        // 2. Các lỗi Multipart khác (VD: quá dung lượng, sai format form-data)
        else if (ex instanceof MultipartException multipart) {
            errors.put("file", "Multipart request error: " + multipart.getMessage());
        }

        return ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .result(errors)
                .build();
    }

    //bắt lỗi runtime
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<String>> handlingAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();
        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .result(now)
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    //lỗi 403
    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<String>> handlingAccessDeniedException(AccessDeniedException exception){
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .result(now)
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }


    //lỗi size, notnull..
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<String>> handlingValidation(MethodArgumentNotValidException exception){
        String enumKey = exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);
            var constraintViolation = exception.getBindingResult()
                    .getAllErrors().getFirst().unwrap(ConstraintViolation.class);

            attributes = constraintViolation.getConstraintDescriptor().getAttributes();
            log.info("attribute {}",attributes.toString());
        } catch (IllegalArgumentException e){
            log.info(e.getMessage());
        }

        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .code(errorCode.getCode())
                .message(Objects.nonNull(attributes)
                        ? mapAttribute(errorCode.getMessage(), attributes)
                        : errorCode.getMessage())
                .result(now)
                .build();

        return ResponseEntity.badRequest().body(apiResponse);
    }


    //map value
    private String mapAttribute(String message, Map<String, Object> attributes) {

        if (attributes.containsKey("value")) {
            String value = String.valueOf(attributes.get("value"));
            message = message.replace("{value}", value);
        }

        if (attributes.containsKey("min")) {
            message = message.replace("{min}",
                    String.valueOf(attributes.get("min")));
        }

        if (attributes.containsKey("max")) {
            message = message.replace("{max}",
                    String.valueOf(attributes.get("max")));
        }

        return message;
    }

//    min max trên params
    //    validate min max
    @ExceptionHandler(value = HandlerMethodValidationException.class)
    ResponseEntity<ApiResponse> handlerMethodValidationException(HandlerMethodValidationException exception){

        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String detailedMessage = exception.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ApiResponse apiResponse = ApiResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(detailedMessage) // Gán message cụ thể
                .result(now)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiResponse);

    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handleMaxSizeException(
            MaxUploadSizeExceededException ex) {

        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(ErrorCode.FILE_TOO_LARGE.getCode())
                .message(ErrorCode.FILE_TOO_LARGE.getMessage())
                .result(now)
                .build();

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE) // 413
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleEnumParseError(IllegalArgumentException ex) {
        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String message = ex.getMessage();
        if (message != null && message.contains("Gender")) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message(ErrorCode.INVALID_GENDER.getMessage())
                            .result(now)
                            .build()
            );
        }

//        lỗi value sort not match
        if (message != null && message.contains("Invalid sort field")) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message(message) // dùng message gốc
                            .result(now)
                            .build()
            );
        }

        if (message != null && message.contains("Sort direction")) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.builder()
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message(message) // dùng message gốc
                            .result(now)
                            .build()
            );
        }

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message(ErrorCode.INVALID_REQUEST.getMessage())
                        .build()
        );
    }

    //    bắt lỗi ngày tháng định dạng yyyy/mm/dd và các lỗi định dạng khác
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {

        String now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        ErrorCode errorCode;

        // Sai định dạng ngày
        if (ex.getMessage() != null && ex.getMessage().contains("LocalDate")) {
            errorCode= ErrorCode.INVALID_FORMAT_DOB;
        }
        // Các lỗi parse JSON hoặc sai kiểu dữ liệu
        else {
            errorCode = ErrorCode.INVALID_DATA_TYPE;
        }

        return ResponseEntity.badRequest().body(ApiResponse.builder().code(errorCode.getCode()).message(errorCode.getMessage()).result(now).build());
    }

}
