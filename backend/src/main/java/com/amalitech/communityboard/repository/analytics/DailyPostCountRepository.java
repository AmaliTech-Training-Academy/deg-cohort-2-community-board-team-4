package com.amalitech.communityboard.repository.analytics;

import com.amalitech.communityboard.model.analytics.DailyPostCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DailyPostCountRepository extends JpaRepository<DailyPostCount, Long> {

    /** Rows from the most recent ETL run, ordered chronologically by day. */
    @Query("SELECT d FROM DailyPostCount d WHERE d.generatedAt = "
            + "(SELECT MAX(x.generatedAt) FROM DailyPostCount x) ORDER BY d.day")
    List<DailyPostCount> findLatestSnapshot();
}
