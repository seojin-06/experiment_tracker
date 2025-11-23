package com.seojin.experiment_tracker.hyperparam.dto;

import com.seojin.experiment_tracker.hyperparam.enums.Source;
import com.seojin.experiment_tracker.hyperparam.enums.ValueType;

public record HyperparamUpsertRequest(
        String key,
        ValueType valueType,
        String valueString,
        Double valueNumeric,
        Boolean valueBoolean,
        String valueJson,
        Source source
) {
}
