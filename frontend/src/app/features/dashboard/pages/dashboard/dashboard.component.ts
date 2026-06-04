import { Component, OnInit, OnDestroy, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { PostService } from '../../../../core/services/post.service';
import { Post, Category } from '../../../../core/models/post.interface';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private postService = inject(PostService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  currentUser = this.authService.currentUser;

  categories = signal<Category[]>([]);
  posts = signal<Post[]>([]);
  totalPosts = signal<number>(0);
  isLoading = signal<boolean>(true);

  // Search & pagination state
  searchQuery = signal<string>('');
  selectedCategoryId = signal<number | undefined>(undefined);
  currentPage = signal<number>(1);
  limit = 4;

  private searchSubject = new Subject<string>();
  private searchSub?: Subscription;
  private queryParamsSub?: Subscription;

  // Date filter signals
  selectedDateOption = signal<string>('all');
  customFromDate = signal<string>('');
  customToDate = signal<string>('');
  isDateDropdownOpen = signal<boolean>(false);

  dateOptions = [
    { value: 'all', label: 'Anytime' },
    { value: '24h', label: 'Last 24 hours' },
    { value: '7d', label: 'Last 7 days' },
    { value: '30d', label: 'Last 30 days' },
    { value: 'custom', label: 'Custom Range...' }
  ];

  selectedDateOptionName = computed(() => {
    const opt = this.dateOptions.find(o => o.value === this.selectedDateOption());
    return opt ? opt.label : 'Anytime';
  });

  // Ticker and modal signals
  ticker = signal<number>(0);
  private tickerIntervalId: any;

  isCreateModalOpen = signal<boolean>(false);
  isSubmittingPost = signal<boolean>(false);
  postError = signal<string>('');
  selectedCategoryIdForPost = signal<number | null>(null);

  postForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    categoryId: ['', Validators.required],
    content: ['', Validators.required]
  });

  isDropdownOpen = signal<boolean>(false);

  selectedCategoryName = computed(() => {
    const id = this.selectedCategoryIdForPost();
    if (!id) return 'Select';
    const cat = this.categories().find(c => c.id === Number(id));
    return cat ? (cat.name === 'Event' ? 'Events' : cat.name) : 'Select';
  });

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
      const categoryName = this.route.snapshot.queryParams['category'] || '';
      if (categoryName) {
        const found = cats.find(c => c.name.toLowerCase() === categoryName.toLowerCase());
        this.selectedCategoryId.set(found ? found.id : undefined);
      }
    });

    this.queryParamsSub = this.route.queryParams.subscribe(params => {
      const keyword = params['keyword'] || '';
      this.searchQuery.set(keyword);

      const categoryName = params['category'] || '';
      if (categoryName && this.categories().length > 0) {
        const found = this.categories().find(c => c.name.toLowerCase() === categoryName.toLowerCase());
        this.selectedCategoryId.set(found ? found.id : undefined);
      } else if (!categoryName) {
        this.selectedCategoryId.set(undefined);
      }

      const dateOption = params['dateOption'] || 'all';
      this.selectedDateOption.set(dateOption);
      
      const from = params['from'] || '';
      this.customFromDate.set(from);

      const to = params['to'] || '';
      this.customToDate.set(to);

      const page = Number(params['page']) || 1;
      this.currentPage.set(page);

      this.loadPostsDirectly(page, keyword, categoryName, from, to);
    });

    this.tickerIntervalId = setInterval(() => {
      this.ticker.update(n => n + 1);
    }, 5000);

    this.searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(val => {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { keyword: val || null, page: 1 },
        queryParamsHandling: 'merge'
      });
    });
  }

  ngOnDestroy(): void {
    if (this.tickerIntervalId) {
      clearInterval(this.tickerIntervalId);
    }
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
    if (this.queryParamsSub) {
      this.queryParamsSub.unsubscribe();
    }
  }

  loadPostsDirectly(page: number, keyword: string, categoryName: string, fromParam?: string, toParam?: string): void {
    this.isLoading.set(true);

    let fromDate = fromParam;
    let toDate = toParam;

    const dateOption = this.selectedDateOption();
    const now = new Date();
    const formatDate = (d: Date): string => {
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      return `${yyyy}-${mm}-${dd}`;
    };

    if (dateOption === '24h') {
      const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
      fromDate = formatDate(yesterday);
      toDate = formatDate(now);
    } else if (dateOption === '7d') {
      const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      fromDate = formatDate(sevenDaysAgo);
      toDate = formatDate(now);
    } else if (dateOption === '30d') {
      const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      fromDate = formatDate(thirtyDaysAgo);
      toDate = formatDate(now);
    }

    this.postService.getPosts(
      page,
      this.limit,
      categoryName || undefined,
      keyword || undefined,
      fromDate || undefined,
      toDate || undefined
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
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { keyword: this.searchQuery() || null, page: 1 },
      queryParamsHandling: 'merge'
    });
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
    this.searchSubject.next(value);
  }

  onClearSearch(): void {
    this.searchQuery.set('');
    this.searchSubject.next('');
  }

  onSelectCategory(catId: number | undefined): void {
    let catName: string | null = null;
    if (catId !== undefined) {
      const found = this.categories().find(c => c.id === catId);
      if (found) {
        catName = found.name;
      }
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { category: catName, page: 1 },
      queryParamsHandling: 'merge'
    });
  }

  onPageChange(page: number): void {
    if (page < 1 || page > this.totalPages()) return;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge'
    });
  }

  toggleDateDropdown(event: Event): void {
    event.stopPropagation();
    this.isDateDropdownOpen.update(v => !v);
  }

  selectDateOption(value: string): void {
    this.isDateDropdownOpen.set(false);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { 
        dateOption: value || null, 
        from: null, 
        to: null, 
        page: 1 
      },
      queryParamsHandling: 'merge'
    });
  }

  onCustomFromDateChange(value: string): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { from: value || null, page: 1 },
      queryParamsHandling: 'merge'
    });
  }

  onCustomToDateChange(value: string): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { to: value || null, page: 1 },
      queryParamsHandling: 'merge'
    });
  }

  closeAllDropdowns(): void {
    this.closeDropdown();
    this.isDateDropdownOpen.set(false);
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
    this.postForm.reset({ title: '', categoryId: '', content: '' });
  }

  closeCreateModal(): void {
    if (this.isSubmittingPost()) return;
    this.isCreateModalOpen.set(false);
    this.postError.set('');
    this.isDropdownOpen.set(false);
    this.selectedCategoryIdForPost.set(null);
    this.postForm.reset();
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

    const { title, content, categoryId } = this.postForm.value;

    this.postService.createPost(title, content, Number(categoryId)).subscribe({
      next: () => {
        this.isSubmittingPost.set(false);
        this.isCreateModalOpen.set(false);
        this.postForm.reset();
        this.selectedCategoryIdForPost.set(null);
        
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { category: null, keyword: null, dateOption: null, from: null, to: null, page: 1 }
        });
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
