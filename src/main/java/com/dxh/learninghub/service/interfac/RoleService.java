package com.dxh.learninghub.service.interfac;

import com.dxh.learninghub.dto.request.RoleRequest;
import com.dxh.learninghub.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {
    RoleResponse create(RoleRequest request);

    List<RoleResponse> getAll();

    void delete(Long role);
}
