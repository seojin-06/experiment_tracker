package com.seojin.experiment_tracker.envsnapshot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojin.experiment_tracker.envsnapshot.domain.EnvSnapshot;
import com.seojin.experiment_tracker.envsnapshot.dto.EnvSnapshotRequest;
import com.seojin.experiment_tracker.envsnapshot.repository.EnvSnapshotRespository;
import com.seojin.experiment_tracker.run.domain.Run;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnvSnapshotService {
    private final EnvSnapshotRespository envSnapshotRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public EnvSnapshot saveOrUpdate(UUID runId, EnvSnapshotRequest req) {
        String libsJson = null;
        String varsJson = null;
        try {
            if (req.libraries() != null) libsJson = objectMapper.writeValueAsString(req.libraries());
            if (req.envVars() != null) varsJson = objectMapper.writeValueAsString(req.envVars());
        } catch (Exception e) {
            throw new RuntimeException("JSON serialize failed", e);
        }

        Run runRef = new Run();
        runRef.setId(runId);

        EnvSnapshot snapshot = envSnapshotRepository.findByRunId(runId)
                .orElseGet(() -> EnvSnapshot.builder().run(runRef).build());

        snapshot.setOsName(req.osName());
        snapshot.setOsVersion(req.osVersion());
        snapshot.setPythonVersion(req.pythonVersion());
        snapshot.setCommitHash(req.commitHash());
        snapshot.setLibrariesJson(libsJson);
        snapshot.setEnvVarsJson(varsJson);

        return envSnapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public Optional<EnvSnapshot> get(UUID runId) {
        return envSnapshotRepository.findByRunId(runId);
    }
}
