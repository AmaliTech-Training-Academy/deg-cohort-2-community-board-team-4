import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { PostService } from '../../../../core/services/post.service';
import { Post, Category } from '../../../../core/models/post.interface';
import { ButtonComponent } from '../../../../core/components/button/button.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, ButtonComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private postService = inject(PostService);
  private router = inject(Router);

  currentUser = this.authService.currentUser;

  categories = signal<Category[]>([]);
  posts = signal<Post[]>([]);
  totalPosts = signal<number>(0);
  isLoading = signal<boolean>(true);

  searchQuery = signal<string>('');
  selectedCategoryId = signal<number | undefined>(undefined);
  currentPage = signal<number>(1);
  limit = 4;

  isMobileMenuOpen = signal<boolean>(false);

  totalPages = computed(() => Math.ceil(this.totalPosts() / this.limit));
  pagesArray = computed(() => {
    const total = this.totalPages();
    const arr = [];
    for (let i = 1; i <= total; i++) arr.push(i);
    return arr;
  });

  ngOnInit(): void {
    this.postService.getCategories().subscribe(cats => {
      this.categories.set(cats);
    });
    this.loadPosts();
  }

  loadPosts(): void {
    this.isLoading.set(true);
    this.postService.getPosts(
      this.currentPage(),
      this.limit,
      this.selectedCategoryId(),
      this.searchQuery()
    ).subscribe({
      next: (res) => {
        this.posts.set(res.posts);
        this.totalPosts.set(res.total);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onSearch(event: Event): void {
    event.preventDefault();
    this.currentPage.set(1);
    this.loadPosts();
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    if (value === '') {
      this.currentPage.set(1);
      this.loadPosts();
    }
  }

  onClearSearch(): void {
    this.searchQuery.set('');
    this.currentPage.set(1);
    this.loadPosts();
  }

  onSelectCategory(catId: number | undefined): void {
    this.selectedCategoryId.set(catId);
    this.currentPage.set(1);
    this.loadPosts();
  }

  onPageChange(page: number): void {
    if (page < 1 || page > this.totalPages()) return;
    this.currentPage.set(page);
    this.loadPosts();
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update(v => !v);
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
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
