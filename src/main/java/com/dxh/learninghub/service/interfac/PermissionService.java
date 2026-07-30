package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.PermissionRequest;
import com.dxh.learninghub.dto.response.PermissionResponse;

import java.util.List;

public interface PermissionService {
    PermissionResponse create(PermissionRequest request);

    List<PermissionResponse> getAll();

    void delete(String permission);
}
