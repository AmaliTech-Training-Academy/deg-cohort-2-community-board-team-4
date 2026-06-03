import { Injectable, inject } from '@angular/core';
import { Observable, of, timer } from 'rxjs';
import { delay, switchMap } from 'rxjs/operators';
import { Category, Post, Comment } from '../models/post.interface';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PostService {
  private readonly CATEGORIES_KEY = 'ping_categories';
  private readonly POSTS_KEY = 'ping_posts';
  private readonly COMMENTS_KEY = 'ping_comments';

  private authService = inject(AuthService);

  constructor() {
    this.seedMockData();
  }

  private seedMockData(): void {
    const categoriesSeed: Category[] = [
      { id: 1, name: 'News', description: 'Local community news and updates' },
      { id: 2, name: 'Event', description: 'Upcoming neighborhood gatherings and events' },
      { id: 3, name: 'Discussion', description: 'General topics and conversations' },
      { id: 4, name: 'Alert', description: 'Urgent notices and safety warnings' }
    ];

    const postsSeed: Post[] = [
      {
        id: 1,
        title: 'Community Garden Workday This Saturday',
        slug: 'community-garden-workday-this-saturday',
        content: 'Join us this Saturday at 8 AM for our monthly community garden workday! We\'ll be planting spring vegetables and need volunteers. Bring gloves and water. Coffee and donuts provided!',
        categoryId: 2,
        authorId: 3,
        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 2,
        title: 'Lost: Orange Tabby Cat',
        slug: 'lost-orange-tabby-cat',
        content: 'Our cat Whiskers went missing yesterday evening near Oak Street. He\'s an orange tabby with a white chest, very friendly. Please call 555-0123 if you see him. Reward offered.',
        categoryId: 4,
        authorId: 4,
        createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 3,
        title: 'Best Local Plumber Recommendation?',
        slug: 'best-local-plumber-recommendation',
        content: 'Looking for a reliable plumber to fix a leaky pipe. Does anyone have recommendations for someone trustworthy and reasonably priced in our area?',
        categoryId: 1,
        authorId: 5,
        createdAt: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 4,
        title: 'Need Help Moving Furniture',
        slug: 'need-help-moving-furniture',
        content: 'I\'m moving this weekend and could use help moving some heavy furniture up to a second floor apartment. Happy to provide pizza and drinks! Sunday afternoon works best.',
        categoryId: 3,
        authorId: 3,
        createdAt: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString()
      },
      {
        id: 5,
        title: 'Looking for Dog Walker Recommendations',
        slug: 'looking-for-dog-walker-recommendations',
        content: 'Starting a new job and need someone reliable to walk my golden retriever during lunch hours. Any recommendations for dog walkers in the neighborhood?',
        categoryId: 1,
        authorId: 6,
        createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString()
      }
    ];

    const commentsSeed: Comment[] = [
      { id: 1, postId: 1, authorId: 4, content: 'I will definitely be there! Do we need to bring shovels?', createdAt: new Date(Date.now() - 1.5 * 60 * 60 * 1000).toISOString() },
      { id: 2, postId: 1, authorId: 5, content: 'Sounds fun, I\'ll bring some extra gloves too.', createdAt: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString() },
      { id: 3, postId: 1, authorId: 6, content: 'Count me in for the morning shift.', createdAt: new Date(Date.now() - 0.5 * 60 * 60 * 1000).toISOString() },
      { id: 4, postId: 2, authorId: 3, content: 'Hope you find Whiskers soon! I\'ll keep an eye out.', createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString() },
      { id: 5, postId: 2, authorId: 5, content: 'Shared on my social page.', createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString() },
      { id: 6, postId: 3, authorId: 4, content: 'I highly recommend PlumbPerfect. Fast and very reasonable.', createdAt: new Date(Date.now() - 7 * 60 * 60 * 1000).toISOString() },
      { id: 7, postId: 3, authorId: 3, content: 'Agree, PlumbPerfect is excellent.', createdAt: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString() },
      { id: 8, postId: 3, authorId: 6, content: 'Avoid QuickFix. They overcharge.', createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString() },
      { id: 9, postId: 3, authorId: 4, content: 'We used FlowRight last month, very professional.', createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString() },
      { id: 10, postId: 3, authorId: 5, content: 'Thanks for the recommendations!', createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString() }
    ];

    if (!localStorage.getItem(this.CATEGORIES_KEY)) {
      localStorage.setItem(this.CATEGORIES_KEY, JSON.stringify(categoriesSeed));
    }
    if (!localStorage.getItem(this.POSTS_KEY)) {
      localStorage.setItem(this.POSTS_KEY, JSON.stringify(postsSeed));
    }
    if (!localStorage.getItem(this.COMMENTS_KEY)) {
      localStorage.setItem(this.COMMENTS_KEY, JSON.stringify(commentsSeed));
    }
  }

  private getStoredCategories(): Category[] {
    const data = localStorage.getItem(this.CATEGORIES_KEY);
    return data ? JSON.parse(data) : [];
  }

  private getStoredPosts(): Post[] {
    const data = localStorage.getItem(this.POSTS_KEY);
    return data ? JSON.parse(data) : [];
  }

  private getStoredComments(): Comment[] {
    const data = localStorage.getItem(this.COMMENTS_KEY);
    return data ? JSON.parse(data) : [];
  }

  private getAuthorName(authorId: number): string {
    const users = JSON.parse(localStorage.getItem('ping_users') || '[]');
    const user = users.find((u: any) => u.id === authorId);
    if (user) return user.name;
    
    const staticAuthors: Record<number, string> = {
      3: 'Sarah Johnson',
      4: 'John Smith',
      5: 'Mike Davis',
      6: 'Emma Wilson'
    };
    return staticAuthors[authorId] || 'Anonymous Resident';
  }

  getCategories(): Observable<Category[]> {
    return of(this.getStoredCategories()).pipe(delay(200));
  }

  getPosts(page: number, limit: number, categoryId?: number, search?: string): Observable<{ posts: Post[], total: number }> {
    let posts = this.getStoredPosts();
    const categories = this.getStoredCategories();
    const comments = this.getStoredComments();

    if (categoryId) {
      posts = posts.filter(p => p.categoryId === categoryId);
    }

    if (search && search.trim() !== '') {
      const q = search.toLowerCase();
      posts = posts.filter(p => p.title.toLowerCase().includes(q) || p.content.toLowerCase().includes(q));
    }

    posts.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

    const total = posts.length;
    const startIndex = (page - 1) * limit;
    const paginatedPosts = posts.slice(startIndex, startIndex + limit);

    const resolvedPosts = paginatedPosts.map(p => {
      const category = categories.find(c => c.id === p.categoryId);
      const commentCount = comments.filter(c => c.postId === p.id).length;
      return {
        ...p,
        category,
        author: { id: p.authorId, name: this.getAuthorName(p.authorId) },
        commentCount
      };
    });

    return of({ posts: resolvedPosts, total }).pipe(delay(600));
  }

  getPostById(id: number): Observable<Post> {
    const posts = this.getStoredPosts();
    const categories = this.getStoredCategories();
    const comments = this.getStoredComments();
    const post = posts.find(p => p.id === id);

    if (!post) {
      return timer(300).pipe(
        switchMap(() => { throw new Error('Post not found'); })
      );
    }

    const category = categories.find(c => c.id === post.categoryId);
    const commentCount = comments.filter(c => c.postId === post.id).length;

    const resolved: Post = {
      ...post,
      category,
      author: { id: post.authorId, name: this.getAuthorName(post.authorId) },
      commentCount
    };

    return of(resolved).pipe(delay(400));
  }

  getCommentsByPostId(postId: number): Observable<Comment[]> {
    const comments = this.getStoredComments();
    const filteredComments = comments.filter(c => c.postId === postId);
    
    filteredComments.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

    const resolved = filteredComments.map(c => ({
      ...c,
      author: { id: c.authorId, name: this.getAuthorName(c.authorId) }
    }));

    return of(resolved).pipe(delay(300));
  }

  addComment(postId: number, content: string): Observable<Comment> {
    const comments = this.getStoredComments();
    const currentUser = this.authService.currentUser();
    const authorId = currentUser ? currentUser.id : 1;

    const newComment: Comment = {
      id: comments.length > 0 ? Math.max(...comments.map(c => c.id)) + 1 : 1,
      content,
      postId,
      authorId,
      createdAt: new Date().toISOString()
    };

    comments.push(newComment);
    localStorage.setItem(this.COMMENTS_KEY, JSON.stringify(comments));

    const resolved: Comment = {
      ...newComment,
      author: { id: authorId, name: this.getAuthorName(authorId) }
    };

    return of(resolved).pipe(delay(300));
  }
}
