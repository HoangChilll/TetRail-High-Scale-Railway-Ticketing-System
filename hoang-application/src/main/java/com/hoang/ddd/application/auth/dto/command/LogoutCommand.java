package com.hoang.ddd.application.auth.dto.command;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutCommand {
    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;
}