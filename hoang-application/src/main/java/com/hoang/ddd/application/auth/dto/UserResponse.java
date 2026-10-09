package com.hoang.ddd.application.auth.dto;

import com.hoang.ddd.domain.model.entity.Role;
import com.hoang.ddd.domain.model.entity.User;
import com.hoang.ddd.domain.model.entity.UserStatus;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private UserStatus status;
    private Set<String> roles;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}
