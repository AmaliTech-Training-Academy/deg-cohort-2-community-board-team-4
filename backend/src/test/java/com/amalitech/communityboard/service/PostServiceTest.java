package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.PostRequest;
import com.amalitech.communityboard.dto.PostResponse;
import com.amalitech.communityboard.model.Category;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
import com.amalitech.communityboard.repository.CategoryRepository;
import com.amalitech.communityboard.repository.CommentRepository;
import com.amalitech.communityboard.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    private User author;
    private User otherUser;
    private User admin;
    private Category category;
    private Post post;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id(1L).name("Alice").email("alice@example.com").role(Role.USER)
                .build();
        otherUser = User.builder()
                .id(2L).name("Bob").email("bob@example.com").role(Role.USER)
                .build();
        admin = User.builder()
                .id(3L).name("Carol").email("carol@example.com").role(Role.ADMIN)
                .build();
        category = Category.builder().id(10L).name("General").build();
        post = Post.builder()
                .id(100L).title("Hello").content("World")
                .author(author).category(category)
                .build();
    }

    @Nested
    @DisplayName("getAllPosts")
    class GetAllPosts {

        @Test
        @DisplayName("maps each entity to a PostResponse with the comment count")
        void mapsPostsToResponses() {
            Page<Post> page = new PageImpl<>(List.of(post));
            when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
            when(commentRepository.countByPostId(100L)).thenReturn(4);

            Page<PostResponse> result = postService.getAllPosts(0, 20);

            assertThat(result.getTotalElements()).isEqualTo(1);
            PostResponse response = result.getContent().get(0);
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("Hello");
            assertThat(response.getCategoryName()).isEqualTo("General");
            assertThat(response.getAuthorName()).isEqualTo("Alice");
            assertThat(response.getCommentCount()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("getPostById")
    class GetPostById {

        @Test
        @DisplayName("returns the mapped response when the post exists")
        void returnsResponseWhenFound() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(commentRepository.countByPostId(100L)).thenReturn(0);

            PostResponse result = postService.getPostById(100L);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getAuthorEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("throws when the post does not exist")
        void throwsWhenNotFound() {
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPostById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Post not found");
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("persists the post and attaches the category when categoryId is provided")
        void createsWithCategory() {
            PostRequest request = new PostRequest("New title", "New content", 10L);
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            PostResponse result = postService.createPost(request, author);

            ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(saved.capture());
            assertThat(saved.getValue().getTitle()).isEqualTo("New title");
            assertThat(saved.getValue().getAuthor()).isEqualTo(author);
            assertThat(saved.getValue().getCategory()).isEqualTo(category);
            assertThat(result.getCategoryName()).isEqualTo("General");
        }

        @Test
        @DisplayName("does not look up a category when categoryId is null")
        void createsWithoutCategory() {
            PostRequest request = new PostRequest("New title", "New content", null);
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            PostResponse result = postService.createPost(request, author);

            verify(categoryRepository, never()).findById(any());
            assertThat(result.getCategoryName()).isNull();
            assertThat(result.getCategoryId()).isNull();
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("updates the post when the requester is the author")
        void updatesWhenAuthor() {
            PostRequest request = new PostRequest("Updated", "Updated body", null);
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            PostResponse result = postService.updatePost(100L, request, author);

            assertThat(result.getTitle()).isEqualTo("Updated");
            assertThat(result.getContent()).isEqualTo("Updated body");
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("throws and does not save when the requester is not the author")
        void rejectsNonAuthor() {
            PostRequest request = new PostRequest("Updated", "Updated body", null);
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.updatePost(100L, request, otherUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not authorized to update this post");
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the post does not exist")
        void throwsWhenNotFound() {
            PostRequest request = new PostRequest("Updated", "Updated body", null);
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.updatePost(999L, request, author))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Post not found");
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("deletes when the requester is the author")
        void deletesWhenAuthor() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));

            postService.deletePost(100L, author);

            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("allows an admin who is not the author to delete")
        void deletesWhenAdmin() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));

            postService.deletePost(100L, admin);

            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("throws and does not delete when a non-author, non-admin requests deletion")
        void rejectsNonAuthorNonAdmin() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.deletePost(100L, otherUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not authorized to delete this post");
            verify(postRepository, never()).delete(any());
        }
    }
}
