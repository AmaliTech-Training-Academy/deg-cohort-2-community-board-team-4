import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PostService } from '../../../../core/services/post.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Post, Comment } from '../../../../core/models/post.interface';

@Component({
  selector: 'app-post-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './post-detail.component.html',
  styleUrl: './post-detail.component.scss'
})
export class PostDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private postService = inject(PostService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  currentUser = this.authService.currentUser;

  post = signal<Post | null>(null);
  comments = signal<Comment[]>([]);
  isLoading = signal<boolean>(true);
  commentsLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string>('');

  // Signals for edit mode and custom confirm modal
  editingCommentId = signal<number | null>(null);
  editingText = signal<string>('');
  commentToDelete = signal<number | null>(null);
  isDeletingComment = signal<boolean>(false);
  deleteError = signal<string>('');

  commentForm: FormGroup = this.fb.group({
    content: ['', [Validators.required, Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loadPost(idParam);
  }

  loadPost(identifier: string | number): void {
    this.postService.getPostById(identifier).subscribe({
      next: (data) => {
        this.post.set(data);
        this.isLoading.set(false);
        this.loadComments(data.id);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Post not found');
        this.isLoading.set(false);
      }
    });
  }

  loadComments(postId: number): void {
    this.postService.getCommentsByPostId(postId).subscribe({
      next: (data) => {
        this.comments.set(data);
        this.commentsLoading.set(false);
      },
      error: () => {
        this.commentsLoading.set(false);
      }
    });
  }

  onCommentSubmit(): void {
    if (this.commentForm.invalid || !this.post()) return;

    this.isSubmitting.set(true);
    const content = this.commentForm.value.content;
    const postId = this.post()!.id;

    this.postService.addComment(postId, content).subscribe({
      next: (newComment) => {
        this.comments.update(all => [...all, newComment]);
        this.commentForm.reset();
        this.isSubmitting.set(false);
        this.post.update(p => p ? { ...p, commentCount: (p.commentCount || 0) + 1 } : null);
      },
      error: () => {
        this.isSubmitting.set(false);
      }
    });
  }

  onStartEditComment(comment: Comment): void {
    this.editingCommentId.set(comment.id);
    this.editingText.set(comment.content);
    setTimeout(() => {
      const textarea = document.querySelector('.comment-textarea.edit-mode') as HTMLTextAreaElement;
      if (textarea) {
        this.adjustHeight(textarea);
      }
    }, 0);
  }

  onEditingTextInput(value: string, textareaEl: HTMLTextAreaElement): void {
    this.editingText.set(value);
    this.adjustHeight(textareaEl);
  }

  private adjustHeight(textareaEl: HTMLTextAreaElement): void {
    textareaEl.style.height = 'auto';
    textareaEl.style.height = `${textareaEl.scrollHeight}px`;
  }

  onCancelEdit(): void {
    this.editingCommentId.set(null);
    this.editingText.set('');
  }

  onSaveEditComment(commentId: number): void {
    const text = this.editingText().trim();
    if (!text) return;

    this.comments.update(all => all.map(c => c.id === commentId ? { ...c, content: text } : c));
    this.editingCommentId.set(null);
    this.editingText.set('');
  }

  onConfirmDeleteComment(commentId: number): void {
    this.commentToDelete.set(commentId);
    this.deleteError.set('');
    this.isDeletingComment.set(false);
  }

  onCancelDeleteComment(): void {
    if (this.isDeletingComment()) return;
    this.commentToDelete.set(null);
    this.deleteError.set('');
  }

  onExecuteDeleteComment(): void {
    const commentId = this.commentToDelete();
    if (!commentId) return;

    this.isDeletingComment.set(true);
    this.deleteError.set('');

    this.postService.deleteComment(commentId).subscribe({
      next: () => {
        this.comments.update(all => all.filter(c => c.id !== commentId));
        this.post.update(p => p ? { ...p, commentCount: Math.max(0, (p.commentCount || 1) - 1) } : null);
        this.isDeletingComment.set(false);
        this.commentToDelete.set(null);
      },
      error: (err) => {
        this.isDeletingComment.set(false);
        this.deleteError.set(err.error?.message || 'Unauthorized to delete this comment');
      }
    });
  }

  getRelativeTime(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `about ${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `about ${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  }
}
