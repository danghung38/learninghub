package com.dxh.learninghub.mapper;


import com.dxh.learninghub.dto.request.RoleRequest;
import com.dxh.learninghub.dto.response.RoleResponse;
import com.dxh.learninghub.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {
    //bỏ qua k map Set<permission> vì list nhận vào là String
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}