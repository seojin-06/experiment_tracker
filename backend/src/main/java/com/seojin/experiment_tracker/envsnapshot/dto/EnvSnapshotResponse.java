package com.seojin.experiment_tracker.envsnapshot.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojin.experiment_tracker.envsnapshot.domain.EnvSnapshot;

import java.util.Map;

public record EnvSnapshotResponse(
        String osName,
        String osVersion,
        String pythonVersion,
        String commitHash,
        Map<String, String> libraries,
        Map<String, String> envVars
) {
    public static EnvSnapshotResponse from(EnvSnapshot e, ObjectMapper om) {
        try {
            Map<String,String> libs = e.getLibrariesJson()!=null
                    ? om.readValue(e.getLibrariesJson(), Map.class) : null;
            Map<String,String> vars = e.getEnvVarsJson()!=null
                    ? om.readValue(e.getEnvVarsJson(), Map.class) : null;
            return new EnvSnapshotResponse(
                    e.getOsName(), e.getOsVersion(), e.getPythonVersion(),
                    e.getCommitHash(), libs, vars
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse EnvSnapshot JSON", ex);
        }
    }
}
