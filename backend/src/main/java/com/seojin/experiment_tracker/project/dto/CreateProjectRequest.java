package com.seojin.experiment_tracker.project.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(@NotBlank String projectName, String description) {
}
