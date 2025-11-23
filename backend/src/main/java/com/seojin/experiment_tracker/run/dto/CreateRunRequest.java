package com.seojin.experiment_tracker.run.dto;

import com.seojin.experiment_tracker.run.enums.RunStatus;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateRunRequest(
        @NotNull UUID projectId,
        UUID experimentId,
        RunStatus status,
        Integer seed,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long elapsedMs,
        String notes
) {
}
