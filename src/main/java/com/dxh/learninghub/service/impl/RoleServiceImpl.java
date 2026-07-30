package com.dxh.learninghub.service.impl;


import com.dxh.learninghub.dto.request.RoleRequest;
import com.dxh.learninghub.dto.response.RoleResponse;
import com.dxh.learninghub.mapper.RoleMapper;
import com.dxh.learninghub.repo.PermissionRepository;
import com.dxh.learninghub.repo.RoleRepository;
import com.dxh.learninghub.service.interfac.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public RoleResponse create(RoleRequest request){
        var role = roleMapper.toRole(request);

        var permissions = permissionRepository.findAllById(request.permissions());
        role.setPermissions(new HashSet<>(permissions));

        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<RoleResponse> getAll(){
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toRoleResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(Long role){
        roleRepository.deleteById(role);
    }
}
