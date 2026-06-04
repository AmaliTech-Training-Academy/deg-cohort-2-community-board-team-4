import { Injectable, signal, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { Category, Post, Comment } from '../models/post.interface';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PostService {
  private readonly API_URL = `${environment.apiUrl}/posts`;
  private http = inject(HttpClient);

  // Private store state signals
  private postsState = signal<Post[]>([]);
  private categoriesState = signal<Category[]>([]);

  // Public read-only signals for store consumption
  posts = this.postsState.asReadonly();
  categories = this.categoriesState.asReadonly();

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${environment.apiUrl}/categories`).pipe(
      tap(cats => {
        this.categoriesState.set(cats || []);
      })
    );
  }

  getPosts(
    page: number, 
    limit: number, 
    categoryName?: string, 
    search?: string,
    fromDate?: string,
    toDate?: string
  ): Observable<{ posts: Post[], total: number }> {
    let params = new HttpParams()
      .set('page', (page - 1).toString()) // Spring Pageable is 0-indexed
      .set('size', limit.toString());

    if (search && search.trim() !== '') {
      params = params.set('keyword', search.trim());
    }

    if (categoryName && categoryName.trim() !== '') {
      params = params.set('category', categoryName.trim());
    }

    if (fromDate) {
      params = params.set('from', fromDate);
    }

    if (toDate) {
      params = params.set('to', toDate);
    }

    return this.http.get<any>(this.API_URL, { params }).pipe(
      map(res => {
        const posts = (res.content || []).map((item: any) => this.mapPostResponse(item));
        const total = res.totalElements || 0;
        
        // Update internal store state
        this.postsState.set(posts);
        
        return { posts, total };
      })
    );
  }

  getPostById(id: number | string): Observable<Post> {
    return this.http.get<any>(`${this.API_URL}/${id}`).pipe(
      map(res => this.mapPostResponse(res))
    );
  }

  getCommentsByPostId(postId: number): Observable<Comment[]> {
    return this.http.get<any[]>(`${this.API_URL}/${postId}/comments`).pipe(
      map(comments => (comments || []).map(c => this.mapCommentResponse(c)))
    );
  }

  addComment(postId: number, content: string): Observable<Comment> {
    return this.http.post<any>(`${this.API_URL}/${postId}/comments`, { content }).pipe(
      map(res => this.mapCommentResponse(res))
    );
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/comments/${commentId}`);
  }

  createPost(title: string, content: string, categoryId: number, image?: File): Observable<Post> {
    const formData = new FormData();
    formData.append('title', title);
    formData.append('content', content);
    formData.append('categoryId', categoryId.toString());
    if (image) {
      formData.append('image', image);
    }

    // Let the browser set the multipart Content-Type (with boundary); do not set it manually.
    return this.http.post<any>(this.API_URL, formData).pipe(
      map(res => this.mapPostResponse(res))
    );
  }


  // --- Backend DTO Mappers ---

  private mapPostResponse(res: any): Post {
    return {
      id: res.id,
      title: res.title,
      slug: res.slug,
      content: res.content,
      imageUrl: res.imageUrl,
      categoryId: res.categoryId,
      authorId: 0,
      createdAt: res.createdAt,
      updatedAt: res.updatedAt,
      category: {
        id: res.categoryId,
        name: res.categoryName
      },
      author: {
        id: 0,
        name: res.authorName
      },
      commentCount: res.commentCount
    };
  }

  private mapCommentResponse(res: any): Comment {
    return {
      id: res.id,
      content: res.content,
      postId: 0, 
      authorId: 0, 
      createdAt: res.createdAt,
      author: {
        id: 0,
        name: res.authorName
      }
    };
  }
}
