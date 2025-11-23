package com.seojin.experiment_tracker.run.dto;

import com.seojin.experiment_tracker.run.domain.Run;

import java.time.OffsetDateTime;

public record RunResponse(
        String id,
        String projectId,
        String experimentId,
        String status,
        Integer seed,
        String startedAt,
        String finishedAt,
        Long elapsedMs,
        String notes
) {
    public static RunResponse of(Run r) {
        return new RunResponse(
                r.getId() != null ? r.getId().toString() : null,
                r.getProject() != null ? r.getProject().getId().toString() : null,
                r.getExperiment() != null ? r.getExperiment().getId().toString() : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getSeed(),
                ts(r.getStartedAt()),
                ts(r.getFinishedAt()),
                r.getElapsedMs(),
                r.getNotes()
        );
    }
    private static String ts(OffsetDateTime t){
        return t!=null ? t.toString() : null;
    }
}
