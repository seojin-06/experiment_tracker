package com.seojin.experiment_tracker.datasetref.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record DatasetRefRequest(
        @NotBlank String name,
        String version,
        @NotBlank String uri,
        String checksum,
        Long sizeBytes,
        String description,
        UUID runId
) {
}
