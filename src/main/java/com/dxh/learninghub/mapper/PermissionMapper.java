package com.dxh.learninghub.mapper;



import com.dxh.learninghub.dto.request.PermissionRequest;
import com.dxh.learninghub.dto.response.PermissionResponse;
import com.dxh.learninghub.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);
    PermissionResponse toPermissionResponse(Permission permission);
}
