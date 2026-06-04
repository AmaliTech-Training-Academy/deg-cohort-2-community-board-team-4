package com.amalitech.communityboard.model.analytics;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "top_users", schema = "analytics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TopUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "post_count", nullable = false)
    private Integer postCount;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
