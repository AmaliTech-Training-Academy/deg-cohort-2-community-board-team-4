package com.amalitech.communityboard.mapper;

import com.amalitech.communityboard.dto.AuthResponse;
import com.amalitech.communityboard.dto.RegisterRequest;
import com.amalitech.communityboard.dto.UserResponse;
import com.amalitech.communityboard.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserResponse toUserResponse(User user);
    @Mapping(target = "password", ignore = true)
    User toEntity(RegisterRequest request);
    AuthResponse toAuthResponse(User user, String token);
}
