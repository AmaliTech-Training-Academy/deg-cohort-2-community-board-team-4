package com.amalitech.communityboard.model.analytics;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "summary", schema = "analytics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyticsSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_posts", nullable = false)
    private Integer totalPosts;

    @Column(name = "total_comments", nullable = false)
    private Integer totalComments;

    @Column(name = "total_users", nullable = false)
    private Integer totalUsers;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
