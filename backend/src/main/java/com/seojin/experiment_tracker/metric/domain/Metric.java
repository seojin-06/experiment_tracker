package com.seojin.experiment_tracker.metric.domain;

import com.seojin.experiment_tracker.metric.dto.LogMetricsRequest;
import com.seojin.experiment_tracker.run.domain.Run;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "metric", indexes = {
        @Index(name = "idx_metric_run_key_step", columnList = "run_id,key,step")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Metric { //학습 중 기록되는 지표 시계열
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private Run run;

    @Column(nullable = false)
    private Long step;

    @Column(length = 100, nullable = false)
    private String key;

    @Column(name = "value_numeric", nullable = false)
    private Double value;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

}
