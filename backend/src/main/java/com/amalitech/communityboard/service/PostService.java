package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.*;
import com.amalitech.communityboard.model.*;
import com.amalitech.communityboard.exception.ForbiddenException;
import com.amalitech.communityboard.exception.ResourceNotFoundException;
import com.amalitech.communityboard.repository.*;
import com.amalitech.communityboard.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public Page<PostResponse> getAllPosts(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<Long, Integer> commentCounts = commentCountsFor(posts.getContent());
        return posts.map(post -> toResponse(post, commentCounts.getOrDefault(post.getId(), 0)));
    }

    private Map<Long, Integer> commentCountsFor(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, Integer> counts = new HashMap<>();
        for (Object[] row : commentRepository.countByPostIdIn(postIds)) {
            counts.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return counts;
    }

    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        return toResponse(post);
    }

    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with slug " + slug));
        return toResponse(post);
    }

    /** Looks up a post by numeric id, or by slug otherwise. */
    public PostResponse getPostByIdOrSlug(String identifier) {
        if (identifier.matches("\\d+")) {
            return getPostById(Long.parseLong(identifier));
        }
        return getPostBySlug(identifier);
    }

    public PostResponse createPost(PostRequest request, Long userId) {
        User author = getUser(userId);
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

    public PostResponse updatePost(Long id, PostRequest request, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        if (!post.getAuthor().getId().equals(userId)) {
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

    public void deletePost(Long id, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id " + id));
        User requester = getUser(userId);
        if (!post.getAuthor().getId().equals(userId)
                && !requester.getRole().name().equals("ADMIN")) {
            throw new ForbiddenException("Not authorized to delete this post");
        }
        postRepository.delete(post);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
    }

    public Page<PostResponse> searchPosts(String category, String keyword,
                                          LocalDate from, LocalDate to, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Treat blank query params as "no filter".
        String categoryFilter = normalize(category);
        String keywordFilter = normalize(keyword);
        LocalDateTime fromFilter = from != null ? from.atStartOfDay() : null;
        LocalDateTime toFilter = to != null ? to.atTime(LocalTime.MAX) : null;


        Specification<Post> spec = Specification.allOf(
                PostSpecifications.hasCategory(categoryFilter),
                PostSpecifications.matchesKeyword(keywordFilter),
                PostSpecifications.createdFrom(fromFilter),
                PostSpecifications.createdTo(toFilter));

        Page<Post> posts = postRepository.findAll(spec, pageable);
        Map<Long, Integer> commentCounts = commentCountsFor(posts.getContent());
        return posts.map(post -> toResponse(post, commentCounts.getOrDefault(post.getId(), 0)));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

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
        return toResponse(post, commentRepository.countByPostId(post.getId()));
    }

    private PostResponse toResponse(Post post, int commentCount) {
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
                .commentCount(commentCount)
                .build();
    }
}
