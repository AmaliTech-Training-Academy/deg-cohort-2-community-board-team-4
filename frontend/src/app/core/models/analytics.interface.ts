export interface AnalyticsSummary {
  totalPosts: number;
  totalComments: number;
  totalUsers: number;
  generatedAt: string;
}

export interface CategoryCount {
  category: string;
  postCount: number;
}

export interface DailyPostCount {
  day: string; // Format: YYYY-MM-DD
  postCount: number;
}

export interface TopUser {
  userId: number;
  name: string;
  email: string;
  postCount: number;
}

export interface AnalyticsData {
  summary: AnalyticsSummary;
  categoryCounts: CategoryCount[];
  dailyPostCounts: DailyPostCount[];
  topUsers: TopUser[];
}
