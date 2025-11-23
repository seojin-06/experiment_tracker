package com.seojin.experiment_tracker.ai.recommendation.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.seojin.experiment_tracker.ai.recommendation.enums.RecommendationType;
import com.seojin.experiment_tracker.experiment.domain.Experiment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "recommendation", indexes = {
        @Index(name = "idx_reco_experiment_created", columnList = "experiment_id,created_at"),
        @Index(name = "idx_reco_type", columnList = "type")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Recommendation {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    @JsonIgnore
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecommendationType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> paramsJson;

    @Column(name = "predicted_score")
    private Double predictedScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> explanationsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> contextJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
