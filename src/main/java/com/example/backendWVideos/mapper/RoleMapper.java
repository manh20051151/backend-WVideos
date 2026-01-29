package com.example.backendWVideos.mapper;

import com.example.backendWVideos.dto.response.RoleResponse;
import com.example.backendWVideos.entity.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {PermissionMapper.class})
public interface RoleMapper {
    RoleResponse toRoleResponse(Role role);
}
