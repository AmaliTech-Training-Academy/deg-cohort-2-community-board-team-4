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

  commentForm: FormGroup = this.fb.group({
    content: ['', [Validators.required, Validators.maxLength(1000)]]
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.router.navigate(['/dashboard']);
      return;
    }

    const postId = Number(idParam);
    if (isNaN(postId)) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loadPost(postId);
    this.loadComments(postId);
  }

  loadPost(postId: number): void {
    this.postService.getPostById(postId).subscribe({
      next: (data) => {
        this.post.set(data);
        this.isLoading.set(false);
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

  onDeleteComment(commentId: number): void {
    if (confirm('Are you sure you want to delete this comment?')) {
      this.comments.update(all => all.filter(c => c.id !== commentId));
      this.post.update(p => p ? { ...p, commentCount: Math.max(0, (p.commentCount || 1) - 1) } : null);
    }
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
