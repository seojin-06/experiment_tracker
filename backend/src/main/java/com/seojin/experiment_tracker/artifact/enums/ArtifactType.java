package com.seojin.experiment_tracker.artifact.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ArtifactType {
    MODEL,
    CHECKPOINT,
    LOG,
    FIGURE,
    OTHER
}
