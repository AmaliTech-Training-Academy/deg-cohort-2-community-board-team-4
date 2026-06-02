package com.amalitech.communityboard.controller;

import com.amalitech.communityboard.dto.*;
import com.amalitech.communityboard.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getAllPosts(page, size));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PostResponse> getPostBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(postService.getPostBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.createPost(request, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.updatePost(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        postService.deletePost(id, userId);
        return ResponseEntity.noContent().build();
    }

    // TODO: Add search endpoint
    // @GetMapping("/search")
    // public ResponseEntity<Page<PostResponse>> searchPosts(@RequestParam String q, ...) { ... }
}
