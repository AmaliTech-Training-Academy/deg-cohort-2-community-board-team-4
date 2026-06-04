package com.amalitech.communityboard.model.analytics;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_post_counts", schema = "analytics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyPostCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate day;

    @Column(name = "post_count", nullable = false)
    private Integer postCount;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
