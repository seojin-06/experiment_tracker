package com.seojin.experiment_tracker.experiment.dto;

import com.seojin.experiment_tracker.experiment.domain.Experiment;

import java.time.OffsetDateTime;
import java.util.List;

public record ExperimentResponse(String id,
                                 String projectId,
                                 String experimentName,
                                 String purpose,
                                 String notes,
                                 String[] tags,
                                 String createdAt) {
    public static ExperimentResponse of(Experiment e) {
        return new ExperimentResponse(
                e.getId() != null ? e.getId().toString() : null,
                e.getProject() != null ? e.getProject().getId().toString() : null,
                e.getExperimentName(),
                e.getPurpose(),
                e.getNotes(),
                e.getTags(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
        );
    }
}
