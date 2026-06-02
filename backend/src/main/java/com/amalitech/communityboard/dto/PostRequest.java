package com.amalitech.communityboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PostRequest {
    @NotBlank
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;
    @NotBlank
    private String content;
    private Long categoryId;
}
