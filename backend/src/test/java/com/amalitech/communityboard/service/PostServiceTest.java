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
import com.amalitech.communityboard.repository.UserRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
    @Mock
    private UserRepository userRepository;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private PostService postService;

    private static final MultipartFile IMAGE = new MockMultipartFile(
            "image", "pic.jpg", "image/jpeg", new byte[]{1, 2, 3});

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
            when(commentRepository.countByPostIdIn(List.of(100L)))
                    .thenReturn(List.<Object[]>of(new Object[]{100L, 4L}));

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
    @DisplayName("searchPosts")
    class SearchPosts {

        @Test
        @DisplayName("queries via a specification, sorts by createdAt desc, and maps the results")
        void appliesFiltersAndMaps() {
            Page<Post> page = new PageImpl<>(List.of(post));
            when(postRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(commentRepository.countByPostIdIn(List.of(100L)))
                    .thenReturn(List.<Object[]>of(new Object[]{100L, 2L}));

            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 1, 31);
            Page<PostResponse> result = postService.searchPosts("news", "road", from, to, 0, 10);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(postRepository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getPageNumber()).isEqualTo(0);
            assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCommentCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("works with no filters (blank/null) and clamps page size to the max")
        void worksWithNoFilters() {
            Page<Post> page = new PageImpl<>(List.of(post));
            when(postRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(commentRepository.countByPostIdIn(List.of(100L)))
                    .thenReturn(List.<Object[]>of(new Object[]{100L, 0L}));

            Page<PostResponse> result = postService.searchPosts("  ", "", null, null, 0, 9999);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(postRepository).findAll(any(Specification.class), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100); // clamped to MAX_PAGE_SIZE
            assertThat(result.getTotalElements()).isEqualTo(1);
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
                    .hasMessage("Post not found with id 999");
        }
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("persists the post and attaches the category when categoryId is provided")
        void createsWithCategory() {
            PostRequest request = new PostRequest("New title", "New content", 10L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(cloudinaryService.upload(any()))
                    .thenReturn(new ImageUploadResult("https://img/pic.jpg", "posts/pic"));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            PostResponse result = postService.createPost(request, IMAGE, 1L);

            ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(saved.capture());
            assertThat(saved.getValue().getTitle()).isEqualTo("New title");
            assertThat(saved.getValue().getAuthor()).isEqualTo(author);
            assertThat(saved.getValue().getCategory()).isEqualTo(category);
            assertThat(saved.getValue().getImageUrl()).isEqualTo("https://img/pic.jpg");
            assertThat(result.getCategoryName()).isEqualTo("General");
            assertThat(result.getImageUrl()).isEqualTo("https://img/pic.jpg");
        }

        @Test
        @DisplayName("throws and does not save when the category does not exist")
        void throwsWhenCategoryNotFound() {
            PostRequest request = new PostRequest("New title", "New content", 99L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.createPost(request, IMAGE, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Category not found with id 99");
            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("updates the post when the requester is the author")
        void updatesWhenAuthor() {
            PostRequest request = new PostRequest("Updated", "Updated body", 10L);
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

            PostResponse result = postService.updatePost(100L, request, null, 1L);

            assertThat(result.getTitle()).isEqualTo("Updated");
            assertThat(result.getContent()).isEqualTo("Updated body");
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("throws and does not save when the requester is not the author")
        void rejectsNonAuthor() {
            PostRequest request = new PostRequest("Updated", "Updated body", null);
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.updatePost(100L, request, null, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not authorized to update this post");
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the post does not exist")
        void throwsWhenNotFound() {
            PostRequest request = new PostRequest("Updated", "Updated body", null);
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.updatePost(999L, request, null, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Post not found with id 999");
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("deletes when the requester is the author")
        void deletesWhenAuthor() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));

            postService.deletePost(100L, 1L);

            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("allows an admin who is not the author to delete")
        void deletesWhenAdmin() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));

            postService.deletePost(100L, 3L);

            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("throws and does not delete when a non-author, non-admin requests deletion")
        void rejectsNonAuthorNonAdmin() {
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> postService.deletePost(100L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not authorized to delete this post");
            verify(postRepository, never()).delete(any(Post.class));
        }
    }
}
