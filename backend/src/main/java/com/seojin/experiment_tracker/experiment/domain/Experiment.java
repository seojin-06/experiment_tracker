package com.seojin.experiment_tracker.experiment.domain;

import com.seojin.experiment_tracker.project.domain.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "experiment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Experiment {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(length = 120, nullable = false)
    private String experimentName;

    @Column(columnDefinition = "text")
    private String purpose;

    @Column(columnDefinition = "text")
    private String notes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
