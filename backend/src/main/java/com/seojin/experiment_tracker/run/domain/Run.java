package com.seojin.experiment_tracker.run.domain;

import com.seojin.experiment_tracker.experiment.domain.Experiment;
import com.seojin.experiment_tracker.project.domain.Project;
import com.seojin.experiment_tracker.run.enums.RunStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "run", indexes = {
        @Index(name = "idx_run_experiment", columnList = "experiment_id"),
        @Index(name = "idx_run_project", columnList = "project_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Run {
    //연구 코드가 학습 시작 직전에 POST /experiments/{eid}/runs 호출
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RunStatus status = RunStatus.PENDING;

    private Integer seed;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    @Column(columnDefinition = "text")
    private String notes;
}
