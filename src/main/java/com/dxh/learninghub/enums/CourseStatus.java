package com.dxh.learninghub.enums;

public enum CourseStatus {
    DRAFT,     // Bản nháp đang được biên soạn
    PENDING,   // Chờ duyệt
    APPROVED,  // Đã duyệt
    REJECTED,  // Từ chối
    BANNED,    // Đã duyệt nhưng bị khóa
    DELETED    // Tác giả/admin đã xóa mềm
}
