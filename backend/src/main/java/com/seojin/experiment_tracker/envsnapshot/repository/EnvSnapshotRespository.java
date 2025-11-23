package com.seojin.experiment_tracker.envsnapshot.repository;

import com.seojin.experiment_tracker.envsnapshot.domain.EnvSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnvSnapshotRespository extends JpaRepository<EnvSnapshot, UUID> {
    Optional<EnvSnapshot> findByRunId(UUID runId);
}
