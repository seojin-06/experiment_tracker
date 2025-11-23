package com.seojin.experiment_tracker.datasetref.repository;

import com.seojin.experiment_tracker.datasetref.domain.DatasetRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetRefRepository extends JpaRepository<DatasetRef, UUID> {
    List<DatasetRef> findByExperiment_IdOrderByCreatedAtDesc(UUID experimentId);
    List<DatasetRef> findByRun_IdOrderByCreatedAtDesc(UUID runId);
}
