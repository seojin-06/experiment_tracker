package com.seojin.experiment_tracker.envsnapshot.dto;

import java.util.Map;

public record EnvSnapshotRequest(
        String osName,
        String osVersion,
        String pythonVersion,
        String commitHash,
        Map<String, String> libraries,
        Map<String, String> envVars
) {
}
