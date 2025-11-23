package com.seojin.experiment_tracker.project.dto;

import com.seojin.experiment_tracker.project.domain.Project;

public record ProjectResponse(String id, String projectName, String description, String createdAt) {
    public static ProjectResponse of(com.seojin.experiment_tracker.project.domain.Project p) {
        return new ProjectResponse(
                p.getId().toString(),
                p.getProjectName(),
                p.getDescription(),
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : null
        );
    }
}
