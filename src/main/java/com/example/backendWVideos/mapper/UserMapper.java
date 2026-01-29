package com.example.backendWVideos.mapper;


import com.example.backendWVideos.dto.request.UserCreateRequest;
import com.example.backendWVideos.dto.request.UserUpdateByUserRequest;
import com.example.backendWVideos.dto.request.UserUpdateRequest;
import com.example.backendWVideos.dto.response.UserResponse;
import com.example.backendWVideos.entity.Document;
import com.example.backendWVideos.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "avatar", ignore = true)
    User toUser (UserCreateRequest request);
    
@Mapping(target = "purchasedDocumentIds", source = "purchasedDocuments", qualifiedByName = "mapDocumentsToIds")
    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateUserByUser(@MappingTarget User user, UserUpdateByUserRequest request);
    
    /**
     * Chuyển đổi Set<Document> purchasedDocuments thành List<String> purchasedDocumentIds
     */
    @Named("mapDocumentsToIds")
    default List<String> mapDocumentsToIds(Set<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of(); // Trả về danh sách rỗng thay vì null
        }
        return documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());
    }
}
