package com.amalitech.communityboard.dto;

import com.amalitech.communityboard.validation.ValidEmail;
import com.amalitech.communityboard.validation.ValidName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @ValidName
    private String name;
    @NotBlank
    @ValidEmail
    private String email;
    @NotBlank @Size(min = 6)
    private String password;
}
