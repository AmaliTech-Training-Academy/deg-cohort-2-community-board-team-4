package com.amalitech.communityboard.dto.analytics;

import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Combined admin analytics payload built from the latest snapshot of each analytics dataset. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsResponse {

    private Summary summary;
    private List<CategoryCount> categoryCounts;
    private List<DailyPostCount> dailyPostCounts;
    private List<TopUser> topUsers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Summary {
        private Integer totalPosts;
        private Integer totalComments;
        private Integer totalUsers;
        private Instant generatedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CategoryCount {
        private String category;
        private Integer postCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DailyPostCount {
        private LocalDate day;
        private Integer postCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TopUser {
        private Long userId;
        private String name;
        private String email;
        private Integer postCount;
    }
}
