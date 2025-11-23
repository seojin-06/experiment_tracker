package com.seojin.experiment_tracker.run.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RunStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELED
}
