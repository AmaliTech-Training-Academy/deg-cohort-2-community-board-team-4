package com.amalitech.communityboard.repository.analytics;

import com.amalitech.communityboard.model.analytics.AnalyticsSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AnalyticsSummaryRepository extends JpaRepository<AnalyticsSummary, Long> {
    Optional<AnalyticsSummary> findTopByOrderByGeneratedAtDesc();
}
