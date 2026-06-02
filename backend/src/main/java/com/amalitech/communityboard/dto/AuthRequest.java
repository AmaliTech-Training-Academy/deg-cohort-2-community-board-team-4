package com.amalitech.communityboard.dto;

import com.amalitech.communityboard.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuthRequest {
    @NotBlank
    @ValidEmail
    private String email;
    @NotBlank
    private String password;
}
