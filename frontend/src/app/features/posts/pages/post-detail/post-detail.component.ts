import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
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
export class PostDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private postService = inject(PostService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  currentUser = this.authService.currentUser;

  ticker = signal<number>(0);
  private tickerIntervalId: any;

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
    this.tickerIntervalId = setInterval(() => {
      this.ticker.update(n => n + 1);
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.tickerIntervalId) {
      clearInterval(this.tickerIntervalId);
    }
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
    this.ticker();

    if (!dateStr) return '';
    let sanitizedDateStr = dateStr;
    if (!dateStr.endsWith('Z') && !dateStr.includes('+') && !/-\d{2}:\d{2}$/.test(dateStr)) {
      sanitizedDateStr = dateStr + 'Z';
    }

    const date = new Date(sanitizedDateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();

    if (diffMs < 2000) return 'just now';

    const diffSecs = Math.floor(diffMs / 1000);
    if (diffSecs < 60) return `${diffSecs}s ago`;

    const diffMins = Math.floor(diffSecs / 60);
    if (diffMins < 60) return `${diffMins}m ago`;

    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;

    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d ago`;
  }
}
