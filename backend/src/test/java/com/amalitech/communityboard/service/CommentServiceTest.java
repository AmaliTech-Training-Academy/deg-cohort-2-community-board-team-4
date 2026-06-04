package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.CommentRequest;
import com.amalitech.communityboard.dto.CommentResponse;
import com.amalitech.communityboard.model.Comment;
import com.amalitech.communityboard.model.Post;
import com.amalitech.communityboard.model.User;
import com.amalitech.communityboard.model.enums.Role;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private User author;
    private User otherUser;
    private User admin;
    private Post post;
    private Comment comment;

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
        post = Post.builder()
                .id(100L).title("Hello").content("World").author(author)
                .build();
        comment = Comment.builder()
                .id(500L).content("Nice post").post(post).author(author)
                .build();
    }

    @Nested
    @DisplayName("getCommentsByPost")
    class GetCommentsByPost {

        @Test
        @DisplayName("maps each comment to a response ordered as returned by the repository")
        void mapsCommentsToResponses() {
            when(postRepository.existsById(100L)).thenReturn(true);
            when(commentRepository.findByPostIdOrderByCreatedAtAsc(100L))
                    .thenReturn(List.of(comment));

            List<CommentResponse> result = commentService.getCommentsByPost(100L);

            assertThat(result).hasSize(1);
            CommentResponse response = result.get(0);
            assertThat(response.getId()).isEqualTo(500L);
            assertThat(response.getContent()).isEqualTo("Nice post");
            assertThat(response.getAuthorName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("throws when the post does not exist")
        void throwsWhenPostNotFound() {
            when(postRepository.existsById(-1L)).thenReturn(false);

            assertThatThrownBy(() -> commentService.getCommentsByPost(-1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Post not found with id -1");
            verify(commentRepository, never()).findByPostIdOrderByCreatedAtAsc(any());
        }
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("persists the comment against the post and authenticated author")
        void createsComment() {
            CommentRequest request = new CommentRequest("A new comment");
            when(postRepository.findById(100L)).thenReturn(Optional.of(post));
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            CommentResponse result = commentService.createComment(100L, request, 1L);

            ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(saved.capture());
            assertThat(saved.getValue().getContent()).isEqualTo("A new comment");
            assertThat(saved.getValue().getPost()).isEqualTo(post);
            assertThat(saved.getValue().getAuthor()).isEqualTo(author);
            assertThat(result.getContent()).isEqualTo("A new comment");
            assertThat(result.getAuthorName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("throws and does not save when the target post does not exist")
        void throwsWhenPostNotFound() {
            CommentRequest request = new CommentRequest("A new comment");
            when(postRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(999L, request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Post not found with id 999");
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("deletes when the requester is the comment author")
        void deletesWhenAuthor() {
            when(commentRepository.findById(500L)).thenReturn(Optional.of(comment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));

            commentService.deleteComment(500L, 1L);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("allows an admin who is not the author to delete")
        void deletesWhenAdmin() {
            when(commentRepository.findById(500L)).thenReturn(Optional.of(comment));
            when(userRepository.findById(3L)).thenReturn(Optional.of(admin));

            commentService.deleteComment(500L, 3L);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("allows the post owner to delete a comment on their post")
        void deletesWhenPostOwner() {
            Comment othersComment = Comment.builder()
                    .id(501L).content("From Bob").post(post).author(otherUser)
                    .build();
            when(commentRepository.findById(501L)).thenReturn(Optional.of(othersComment));
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));

            commentService.deleteComment(501L, 1L);

            verify(commentRepository).delete(othersComment);
        }

        @Test
        @DisplayName("throws and does not delete when a non-author, non-admin requests deletion")
        void rejectsNonAuthorNonAdmin() {
            when(commentRepository.findById(500L)).thenReturn(Optional.of(comment));
            when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> commentService.deleteComment(500L, 2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Not authorized to delete this comment");
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws when the comment does not exist")
        void throwsWhenNotFound() {
            when(commentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(999L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Comment not found with id 999");
            verify(commentRepository, never()).delete(any());
        }
    }
}
