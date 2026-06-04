package com.amalitech.communityboard.service;

import com.amalitech.communityboard.dto.analytics.AnalyticsResponse;
import com.amalitech.communityboard.repository.analytics.AnalyticsSummaryRepository;
import com.amalitech.communityboard.repository.analytics.CategoryCountRepository;
import com.amalitech.communityboard.repository.analytics.DailyPostCountRepository;
import com.amalitech.communityboard.repository.analytics.TopUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsSummaryRepository summaryRepository;
    private final CategoryCountRepository categoryCountRepository;
    private final DailyPostCountRepository dailyPostCountRepository;
    private final TopUserRepository topUserRepository;

    /** Assembles the latest snapshot of every analytics dataset into a single payload. */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {
        AnalyticsResponse.Summary summary = summaryRepository.findTopByOrderByGeneratedAtDesc()
                .map(s -> AnalyticsResponse.Summary.builder()
                        .totalPosts(s.getTotalPosts())
                        .totalComments(s.getTotalComments())
                        .totalUsers(s.getTotalUsers())
                        .generatedAt(s.getGeneratedAt())
                        .build())
                .orElse(null);

        var categoryCounts = categoryCountRepository.findLatestSnapshot().stream()
                .map(c -> AnalyticsResponse.CategoryCount.builder()
                        .category(c.getCategory())
                        .postCount(c.getPostCount())
                        .build())
                .toList();

        var dailyPostCounts = dailyPostCountRepository.findLatestSnapshot().stream()
                .map(d -> AnalyticsResponse.DailyPostCount.builder()
                        .day(d.getDay())
                        .postCount(d.getPostCount())
                        .build())
                .toList();

        var topUsers = topUserRepository.findLatestSnapshot().stream()
                .map(u -> AnalyticsResponse.TopUser.builder()
                        .userId(u.getUserId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .postCount(u.getPostCount())
                        .build())
                .toList();

        return AnalyticsResponse.builder()
                .summary(summary)
                .categoryCounts(categoryCounts)
                .dailyPostCounts(dailyPostCounts)
                .topUsers(topUsers)
                .build();
    }
}
