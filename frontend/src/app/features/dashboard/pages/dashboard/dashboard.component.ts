import { Component, OnInit, OnDestroy, signal, computed, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { QuillEditorComponent } from 'ngx-quill';
import { AuthService } from '../../../../core/services/auth.service';
import { PostService } from '../../../../core/services/post.service';
import { Post, Category } from '../../../../core/models/post.interface';

/** Treats HTML whose visible text is empty (e.g. Quill's "<p><br></p>") as required-failing. */
function htmlNotBlankValidator(control: AbstractControl): ValidationErrors | null {
  const value = (control.value as string | null) ?? '';
  const text = value.replace(/<[^>]*>/g, '').replace(/&nbsp;/gi, ' ').trim();
  return text.length > 0 ? null : { required: true };
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, QuillEditorComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private postService = inject(PostService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  currentUser = this.authService.currentUser;

  ticker = signal<number>(0);
  private tickerIntervalId: any;

  categories = signal<Category[]>([]);
  posts = signal<Post[]>([]);
  totalPosts = signal<number>(0);
  isLoading = signal<boolean>(true);

  // Post creation modal signals and form
  isCreateModalOpen = signal<boolean>(false);
  isSubmittingPost = signal<boolean>(false);
  postError = signal<string>('');
  selectedCategoryIdForPost = signal<number | null>(null);

  postForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: ['', Validators.required],
    content: ['', htmlNotBlankValidator],
    image: [null as File | null, Validators.required]
  });

  // Mirror the backend's accepted types and 5MB limit so users get instant feedback.
  private readonly allowedImageTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
  private readonly maxImageSize = 5 * 1024 * 1024;

  imagePreviewUrl = signal<string | null>(null);
  imageError = signal<string>('');

  @ViewChild('imageInput') imageInput?: ElementRef<HTMLInputElement>;

  isDropdownOpen = signal<boolean>(false);

  selectedCategoryName = computed(() => {
    const id = this.selectedCategoryIdForPost();
    if (!id) return 'Select';
    const cat = this.categories().find(c => c.id === Number(id));
    return cat ? (cat.name === 'Event' ? 'Events' : cat.name) : 'Select';
  });

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
    this.tickerIntervalId = setInterval(() => {
      this.ticker.update(n => n + 1);
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.tickerIntervalId) {
      clearInterval(this.tickerIntervalId);
    }
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

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
    this.postError.set('');
    this.isDropdownOpen.set(false);
    this.selectedCategoryIdForPost.set(null);
    this.postForm.reset({ title: '', categoryId: '', content: '', image: null });
    this.clearImage();
  }

  closeCreateModal(): void {
    if (this.isSubmittingPost()) return;
    this.isCreateModalOpen.set(false);
    this.postError.set('');
    this.isDropdownOpen.set(false);
    this.selectedCategoryIdForPost.set(null);
    this.postForm.reset();
    this.clearImage();
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.imageError.set('');

    if (!file) {
      return;
    }
    if (!this.allowedImageTypes.includes(file.type)) {
      this.imageError.set('Unsupported image type. Use JPEG, PNG, WEBP or GIF.');
      this.clearImage();
      return;
    }
    if (file.size > this.maxImageSize) {
      this.imageError.set('Image is too large. Maximum size is 5MB.');
      this.clearImage();
      return;
    }

    this.postForm.get('image')?.setValue(file);
    this.postForm.get('image')?.markAsTouched();

    const reader = new FileReader();
    reader.onload = () => this.imagePreviewUrl.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  removeImage(): void {
    this.imageError.set('');
    this.clearImage();
    this.postForm.get('image')?.markAsTouched();
  }

  private clearImage(): void {
    this.postForm.get('image')?.setValue(null);
    this.imagePreviewUrl.set(null);
    if (this.imageInput) {
      this.imageInput.nativeElement.value = '';
    }
  }

  closeDropdown(): void {
    if (this.isDropdownOpen()) {
      this.isDropdownOpen.set(false);
      this.postForm.get('categoryId')?.markAsTouched();
    }
  }

  toggleDropdown(event: Event): void {
    event.stopPropagation();
    if (this.isDropdownOpen()) {
      this.isDropdownOpen.set(false);
      this.postForm.get('categoryId')?.markAsTouched();
    } else {
      this.isDropdownOpen.set(true);
    }
  }

  selectCategory(categoryId: number): void {
    this.postForm.get('categoryId')?.setValue(categoryId);
    this.postForm.get('categoryId')?.markAsTouched();
    this.selectedCategoryIdForPost.set(categoryId);
    this.isDropdownOpen.set(false);
  }

  onCreatePostSubmit(): void {
    if (this.postForm.invalid || this.isSubmittingPost()) return;

    this.isSubmittingPost.set(true);
    this.postError.set('');

    const { title, content, categoryId, image } = this.postForm.value;

    this.postService.createPost(title, content, Number(categoryId), image).subscribe({
      next: () => {
        this.isSubmittingPost.set(false);
        this.isCreateModalOpen.set(false);
        this.postForm.reset();
        this.clearImage();
        this.selectedCategoryIdForPost.set(null);

        this.selectedCategoryId.set(undefined);
        this.searchQuery.set('');
        this.currentPage.set(1);
        this.loadPosts();
      },
      error: (err) => {
        this.isSubmittingPost.set(false);
        this.postError.set(err.error?.message || 'Could not create post. Please try again.');
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
