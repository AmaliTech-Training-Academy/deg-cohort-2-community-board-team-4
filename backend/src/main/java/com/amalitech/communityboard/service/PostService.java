package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.*;
import com.amalitech.communityboard.model.*;
import com.amalitech.communityboard.exception.ForbiddenException;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.repository.*;
import com.amalitech.communityboard.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;

    public Page<PostResponse> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        return toResponse(post);
    }

    public PostResponse createPost(PostRequest request, User author) {
        Post post = Post.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle()))
                .content(request.getContent())
                .author(author)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .ifPresent(post::setCategory);
        }
        return toResponse(postRepository.save(post));
    }

    public PostResponse updatePost(Long id, PostRequest request, User author) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        if (!post.getAuthor().getId().equals(author.getId())) {
            throw new ForbiddenException("Not authorized to update this post");
        }
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUpdatedAt(LocalDateTime.now());
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .ifPresent(post::setCategory);
        }
        return toResponse(postRepository.save(post));
    }

    public void deletePost(Long id, User author) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        if (!post.getAuthor().getId().equals(author.getId())
                && !author.getRole().name().equals("ADMIN")) {
            throw new ForbiddenException("Not authorized to delete this post");
        }
        postRepository.delete(post);
    }

    // TODO: Implement search functionality
    // public Page<PostResponse> searchPosts(String query, Pageable pageable) { ... }

    private String generateUniqueSlug(String title) {
        String base = SlugUtil.toSlug(title);
        String candidate = base;
        int suffix = 2;
        while (postRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .authorName(post.getAuthor().getName())
                .authorEmail(post.getAuthor().getEmail())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .commentCount(commentRepository.countByPostId(post.getId()))
                .build();
    }
}
