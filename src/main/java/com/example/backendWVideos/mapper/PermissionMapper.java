package com.example.backendWVideos.mapper;

import com.example.backendWVideos.dto.response.PermissionResponse;
import com.example.backendWVideos.entity.Permission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionResponse toPermissionResponse(Permission permission);
}
