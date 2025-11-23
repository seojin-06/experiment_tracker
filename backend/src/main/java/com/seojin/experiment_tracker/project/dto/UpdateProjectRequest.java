package com.seojin.experiment_tracker.project.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectRequest(@NotBlank String projectName,
                                   String description) {
}
