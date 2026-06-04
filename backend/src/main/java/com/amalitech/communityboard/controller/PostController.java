package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.dto.*;
import com.amalitech.communityboard.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "List and search posts",
            description = "Returns a paginated list of posts. All filters are optional and combined with AND: "
                    + "category (case-insensitive name match), keyword (case-insensitive match on title or content), "
                    + "and a created_at date range (from/to, inclusive). The response is a Page that includes the "
                    + "total count alongside the results."
    )
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @Parameter(description = "Filter by category name (case-insensitive)", example = "news")
            @RequestParam(required = false) String category,
            @Parameter(description = "Keyword searched in post title and content (case-insensitive contains)", example = "road")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Start of created_at range, inclusive (yyyy-MM-dd)", example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End of created_at range, inclusive (yyyy-MM-dd)", example = "2024-01-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.searchPosts(category, keyword, from, to, page, size));
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<PostResponse> getPost(@PathVariable String identifier) {
        return ResponseEntity.ok(postService.getPostByIdOrSlug(identifier));
    }

    @Operation(
            summary = "Create a post",
            description = "Creates a post from multipart/form-data. The image part is required."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @Valid @ModelAttribute PostRequest request,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request, image, userId));
    }

    @Operation(
            summary = "Update a post",
            description = "Updates a post from multipart/form-data. The image part is optional; "
                    + "when omitted the existing image is kept."
    )
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @ModelAttribute PostRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.updatePost(id, request, image, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        postService.deletePost(id, userId);
        return ResponseEntity.noContent().build();
    }
}
