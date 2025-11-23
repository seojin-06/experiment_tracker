package com.seojin.experiment_tracker.hyperparam.dto;

import com.seojin.experiment_tracker.hyperparam.domain.Hyperparam;

import java.time.Instant;

public record HyperparamResponse(
        String id,
        String key,
        String valueType,
        String valueString, Double valueNumeric, Boolean valueBoolean, String valueJson,
        String source, Instant createdAt, Instant updatedAt
) {
    public static HyperparamResponse from(Hyperparam h) {
        return new HyperparamResponse(
                h.getId().toString(),
                h.getKey(),
                h.getValueType().name(),
                h.getValueString(),
                h.getValueNumeric(),
                h.getValueBoolean(),
                h.getValueJson(),
                h.getSource().name(),
                h.getCreatedAt(),
                h.getUpdatedAt()
        );
    }
}
