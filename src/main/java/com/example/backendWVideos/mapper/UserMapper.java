package com.example.backendWVideos.mapper;


import com.example.backendWVideos.dto.request.UserCreateRequest;
import com.example.backendWVideos.dto.request.UserUpdateByUserRequest;
import com.example.backendWVideos.dto.request.UserUpdateRequest;
import com.example.backendWVideos.dto.response.UserResponse;
import com.example.backendWVideos.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "avatar", ignore = true)
    User toUser (UserCreateRequest request);
    
    @Mapping(target = "purchasedDocumentIds", ignore = true)
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUserByUser(@MappingTarget User user, UserUpdateByUserRequest request);
}
