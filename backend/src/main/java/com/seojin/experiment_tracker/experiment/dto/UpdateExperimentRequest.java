package com.seojin.experiment_tracker.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateExperimentRequest(@NotBlank String experimentName,
                                      String purpose,
                                      String notes,
                                      @NotNull UUID projectId,
                                      String[] tags) {
}
