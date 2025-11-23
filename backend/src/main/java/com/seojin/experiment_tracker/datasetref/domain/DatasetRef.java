package com.seojin.experiment_tracker.datasetref.domain;

import com.seojin.experiment_tracker.common.jpa.BaseEntity;
import com.seojin.experiment_tracker.experiment.domain.Experiment;
import com.seojin.experiment_tracker.run.domain.Run;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dataset_ref", indexes = {
        @Index(name = "idx_datasetref_experiment", columnList = "experiment_id"),
        @Index(name = "idx_datasetref_run",        columnList = "run_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DatasetRef  extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private Run run;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "version", length = 64)
    private String version;

    @Column(name = "uri", columnDefinition = "text", nullable = false)
    private String uri;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}
