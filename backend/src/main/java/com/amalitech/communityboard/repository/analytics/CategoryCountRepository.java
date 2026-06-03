package com.amalitech.communityboard.repository.analytics;

import com.amalitech.communityboard.model.analytics.CategoryCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CategoryCountRepository extends JpaRepository<CategoryCount, Long> {

    /** Rows from the most recent ETL run, highest post count first. */
    @Query("SELECT c FROM CategoryCount c WHERE c.generatedAt = "
            + "(SELECT MAX(x.generatedAt) FROM CategoryCount x) ORDER BY c.postCount DESC")
    List<CategoryCount> findLatestSnapshot();
}
