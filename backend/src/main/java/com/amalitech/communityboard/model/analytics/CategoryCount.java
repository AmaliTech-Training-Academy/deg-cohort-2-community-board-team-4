package com.amalitech.communityboard.model.analytics;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "category_counts", schema = "analytics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(name = "post_count", nullable = false)
    private Integer postCount;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
