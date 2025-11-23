package com.seojin.experiment_tracker.artifact.domain;

import com.seojin.experiment_tracker.common.jpa.BaseEntity;
import com.seojin.experiment_tracker.run.domain.Run;
import com.seojin.experiment_tracker.artifact.enums.ArtifactType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "artifact", indexes = {
        @Index(name = "idx_artifact_run_type", columnList = "run_id,type")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Artifact extends BaseEntity { //파일 결과물(모델 가중치, 체크포인트, 로그, 그림 등)의 메타데이터
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private Run run;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtifactType type;

    @Column(nullable = false, columnDefinition = "text")
    private String uri;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(length = 128)
    private String checksum;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;
}
