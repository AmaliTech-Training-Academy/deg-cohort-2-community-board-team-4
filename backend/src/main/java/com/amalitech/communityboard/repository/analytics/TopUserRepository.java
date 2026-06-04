package com.amalitech.communityboard.repository.analytics;

import com.amalitech.communityboard.model.analytics.TopUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TopUserRepository extends JpaRepository<TopUser, Long> {

    /** Rows from the most recent ETL run, most active user first. */
    @Query("SELECT t FROM TopUser t WHERE t.generatedAt = "
            + "(SELECT MAX(x.generatedAt) FROM TopUser x) ORDER BY t.postCount DESC")
    List<TopUser> findLatestSnapshot();
}
