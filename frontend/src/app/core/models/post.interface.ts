export interface Category {
  id: number;
  name: string;
  description?: string;
}

export interface Comment {
  id: number;
  content: string;
  postId: number;
  authorId: number;
  createdAt: string;
  updatedAt?: string;
  author?: {
    id: number;
    name: string;
  };
}

export interface Post {
  id: number;
  title: string;
  slug: string;
  content: string;
  imageUrl?: string;
  categoryId: number;
  authorId: number;
  createdAt: string;
  updatedAt?: string;
  category?: Category;
  author?: {
    id: number;
    name: string;
  };
  commentCount?: number;
}
