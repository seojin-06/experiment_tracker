package com.seojin.experiment_tracker.datasetref.dto;

import com.seojin.experiment_tracker.datasetref.domain.DatasetRef;

public record DatasetRefResponse(
        String id,
        String experimentId,
        String runId,
        String name,
        String version,
        String uri,
        String checksum,
        Long sizeBytes,
        String description,
        String createdAt
) {
    public static DatasetRefResponse of(DatasetRef d) {
        return new DatasetRefResponse(
                d.getId() != null ? d.getId().toString() : null,
                d.getExperiment() != null && d.getExperiment().getId() != null
                        ? d.getExperiment().getId().toString() : null,
                d.getRun() != null && d.getRun().getId() != null
                        ? d.getRun().getId().toString() : null,
                d.getName(),
                d.getVersion(),
                d.getUri(),
                d.getChecksum(),
                d.getSizeBytes(),
                d.getDescription(),
                d.getCreatedAt() != null ? d.getCreatedAt().toString() : null
        );
    }
}
