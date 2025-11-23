package com.seojin.experiment_tracker.run.dto;

import com.seojin.experiment_tracker.run.enums.RunStatus;

import java.time.OffsetDateTime;

public record UpdateRunRequest(
        RunStatus status,
        Integer seed,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long elapsedMs,
        String notes
) { }
