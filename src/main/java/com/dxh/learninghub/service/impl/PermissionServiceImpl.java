package com.dxh.learninghub.service.impl;



import com.dxh.learninghub.dto.request.PermissionRequest;
import com.dxh.learninghub.dto.response.PermissionResponse;
import com.dxh.learninghub.entity.Permission;
import com.dxh.learninghub.mapper.PermissionMapper;
import com.dxh.learninghub.repo.PermissionRepository;
import com.dxh.learninghub.service.interfac.PermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionServiceImpl implements PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PermissionResponse create(PermissionRequest request){
        Permission permission = permissionMapper.toPermission(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<PermissionResponse> getAll(){
        var permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toPermissionResponse).toList();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(String permission){
        permissionRepository.deleteById(permission);
    }
}
