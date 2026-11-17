package com.hoang.dto.response;

import com.hoang.model.Role;
import com.hoang.model.User;
import com.hoang.model.UserStatus;
import lombok.*;

import java.util.Set;
import java.util.HashSet;

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
        Set<String> roleNames = new HashSet<>();
        for (Role role : user.getRoles()) {
            roleNames.add(role.getName());
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .roles(roleNames)
                .build();
    }
}
