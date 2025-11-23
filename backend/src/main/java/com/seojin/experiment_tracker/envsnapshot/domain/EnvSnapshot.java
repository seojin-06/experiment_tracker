package com.seojin.experiment_tracker.envsnapshot.domain;

import com.seojin.experiment_tracker.run.domain.Run;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "env_snapshot",
        uniqueConstraints = @UniqueConstraint(name = "uk_envsnapshot_run", columnNames = {"run_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EnvSnapshot {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private Run run;

    @Column(name = "os_name", length = 100)
    private String osName;

    @Column(name = "os_version", length = 100)
    private String osVersion;

    @Column(name = "python_version", length = 50)
    private String pythonVersion;

    @Column(name = "commit_hash", length = 80)
    private String commitHash;

    @Column(name = "libraries_json", columnDefinition = "text")
    private String librariesJson;

    @Column(name = "env_vars_json", columnDefinition = "text")
    private String envVarsJson;
}
