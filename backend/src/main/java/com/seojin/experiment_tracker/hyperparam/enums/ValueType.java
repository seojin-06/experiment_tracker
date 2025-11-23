package com.seojin.experiment_tracker.hyperparam.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    JSON
}
