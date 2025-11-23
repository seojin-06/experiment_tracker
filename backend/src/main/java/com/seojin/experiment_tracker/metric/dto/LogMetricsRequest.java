package com.seojin.experiment_tracker.metric.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record LogMetricsRequest(@NotNull Integer step,
                                @NotNull Map<String, Double> metrics) {
}
