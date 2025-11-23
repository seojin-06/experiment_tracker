package com.seojin.experiment_tracker.runsummary.domain;

import com.seojin.experiment_tracker.common.jpa.BaseEntity;
import com.seojin.experiment_tracker.run.domain.Run;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table( name = "run_summary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_run_summary_run", columnNames = {"run_id"})
        },
        indexes = {
                @Index(name = "idx_run_summary_best_epoch", columnList = "best_epoch"),
                @Index(name = "idx_run_summary_updated_at", columnList = "updated_at")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RunSummary extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private Run run;

    // 관측 기반
    @Column(name = "best_accuracy")
    private Double bestAccuracy;

    @Column(name = "best_epoch")
    private Long bestEpoch;

    @Column(name = "last_epoch")
    private Integer lastEpoch;

    @Column(name = "last_step")
    private Integer lastStep;

    // 예측 기반
    @Column(name = "predicted_final_accuracy")
    private Double predictedFinalAccuracy;

    @Column(name = "early_stop_epoch")
    private Long earlyStopEpoch;

    // 비고(변화점 구간, 분석 메모 등)
    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
