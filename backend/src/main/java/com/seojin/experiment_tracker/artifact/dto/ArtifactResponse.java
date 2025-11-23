package com.seojin.experiment_tracker.artifact.dto;

import com.seojin.experiment_tracker.artifact.domain.Artifact;
import com.seojin.experiment_tracker.artifact.enums.ArtifactType;

import java.util.UUID;

public record ArtifactResponse(
        UUID id,
        String runId,
        ArtifactType type,
        String uri,
        Long sizeBytes,
        String checksum,
        String uploadedAt
) {
    public static ArtifactResponse of(Artifact a) {
        return new ArtifactResponse(
                a.getId(),
                (a.getRun() != null && a.getRun().getId() != null) ? a.getRun().getId().toString() : null,
                a.getType(),
                a.getUri(),
                a.getSizeBytes(),
                a.getChecksum(),
                a.getUploadedAt() != null ? a.getUploadedAt().toString() : null
        );
    }
}
